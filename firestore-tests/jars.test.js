import { after, before, beforeEach, describe, it } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  arrayUnion, collection, deleteDoc, doc, getDoc, getDocs, query, serverTimestamp, updateDoc,
  where, writeBatch,
} from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-swearjar',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});
after(() => env.cleanup());
beforeEach(() => env.clearFirestore());

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();
const CODE = 'ABC234';

const settings = (extra = {}) => ({
  loggingPolicy: 'ANYONE',
  pricingMode: 'FIXED',
  goalBehaviour: 'RESET',
  escalation: { every: 3, multiplierPercent: 200, windowDays: 7, maxSteps: 3 },
  ...extra,
});
const jar = (uid, extra = {}) => ({
  name: 'Flat 4',
  ownerId: uid,
  currency: 'GBP',
  inviteCode: CODE,
  memberIds: [uid],
  settings: settings(),
  currentRoundId: 'r1',
  goalMinor: 2000,
  roundTotalMinor: 0,
  status: 'ACTIVE',
  createdAt: serverTimestamp(),
  ...extra,
});
const member = (role, extra = {}) => ({
  role,
  displayName: 'Someone',
  owedMinor: 0,
  paidMinor: 0,
  joinCode: CODE,
  joinedAt: serverTimestamp(),
  ...extra,
});
const round = (extra = {}) => ({ goalMinor: 2000, totalMinor: 0, startedAt: serverTimestamp(), ...extra });

/** The batch the app writes to create a jar. `o` overrides or drops individual documents. */
function createJar(uid, o = {}) {
  const d = db(uid);
  const b = writeBatch(d);
  const j = { ...jar(uid), ...(o.jar ?? {}) };
  b.set(doc(d, 'jars/j1'), j);
  if (o.code !== null) b.set(doc(d, `inviteCodes/${o.codeId ?? j.inviteCode}`), o.code ?? { jarId: 'j1' });
  if (o.member !== null) b.set(doc(d, `jars/j1/members/${uid}`), o.member ?? member('OWNER'));
  if (o.round !== null) b.set(doc(d, `jars/j1/rounds/${j.currentRoundId}`), o.round ?? round());
  return b.commit();
}

/** The batch the app writes to join a jar. */
function joinJar(uid, o = {}) {
  const d = db(uid);
  const b = writeBatch(d);
  b.update(doc(d, 'jars/j1'), o.jar ?? { memberIds: arrayUnion(uid) });
  if (o.member !== null) b.set(doc(d, `jars/j1/members/${uid}`), o.member ?? member('MEMBER'));
  return b.commit();
}

describe('creating a jar', () => {
  it('works as the app does it', async () => {
    await assertSucceeds(createJar('alice'));
  });

  it('needs the invite code, member and round in the same batch', async () => {
    await assertFails(createJar('alice', { code: null }));
    await assertFails(createJar('alice', { member: null }));
    await assertFails(createJar('alice', { round: null }));
  });

  it('must be owned by, and contain only, its creator', async () => {
    await assertFails(createJar('alice', { jar: { ownerId: 'bob' } }));
    await assertFails(createJar('alice', { jar: { memberIds: ['alice', 'bob'] } }));
    await assertFails(createJar('alice', { jar: { memberIds: ['bob'] } }));
  });

  it('must start empty, active and with a server timestamp', async () => {
    await assertFails(createJar('alice', { jar: { roundTotalMinor: 500 } }));
    await assertFails(createJar('alice', { jar: { status: 'CLOSED' } }));
    await assertFails(createJar('alice', { jar: { createdAt: new Date(0) } }));
  });

  it('validates fields', async () => {
    await assertFails(createJar('alice', { jar: { name: '' } }));
    await assertFails(createJar('alice', { jar: { name: 'x'.repeat(61) } }));
    await assertFails(createJar('alice', { jar: { currency: 'pounds' } }));
    await assertFails(createJar('alice', { jar: { goalMinor: -1 } }));
    await assertFails(createJar('alice', { jar: { goalMinor: 1.5 } }));
    await assertFails(createJar('alice', { jar: { extra: true } }));
    await assertFails(createJar('alice', { jar: { settings: settings({ loggingPolicy: 'NOBODY' }) } }));
    await assertFails(createJar('alice', { jar: { settings: settings({ escalation: { every: 0, multiplierPercent: 200, windowDays: 7, maxSteps: 3 } }) } }));
    await assertFails(createJar('alice', { jar: { inviteCode: 'abc234' }, codeId: 'abc234' }));
    await assertFails(createJar('alice', { jar: { inviteCode: 'ABCO01' }, codeId: 'ABCO01' }));
  });

  it('accepts a jar with no goal', async () => {
    await assertSucceeds(createJar('alice', { jar: { goalMinor: 0 }, round: round({ goalMinor: 0 }) }));
  });

  it('requires the owner member document to be a clean OWNER', async () => {
    await assertFails(createJar('alice', { member: member('MEMBER') }));
    await assertFails(createJar('alice', { member: member('ADMIN') }));
    await assertFails(createJar('alice', { member: member('OWNER', { owedMinor: 100 }) }));
    await assertFails(createJar('alice', { member: member('OWNER', { paidMinor: 100 }) }));
    await assertFails(createJar('alice', { member: member('OWNER', { joinCode: 'ZZZ999' }) }));
  });

  it('requires the round to match the jar', async () => {
    await assertFails(createJar('alice', { round: round({ goalMinor: 1 }) }));
    await assertFails(createJar('alice', { round: round({ totalMinor: 5 }) }));
  });

  it('requires the invite code document to point at the jar', async () => {
    await assertFails(createJar('alice', { code: { jarId: 'other' } }));
    await assertFails(createJar('alice', { codeId: 'ZZZ999' }));
  });

  it('cannot reuse an invite code that is taken', async () => {
    await assertSucceeds(createJar('alice'));
    const d = db('bob');
    const b = writeBatch(d);
    b.set(doc(d, 'jars/j2'), jar('bob'));
    b.set(doc(d, `inviteCodes/${CODE}`), { jarId: 'j2' });
    b.set(doc(d, 'jars/j2/members/bob'), member('OWNER'));
    b.set(doc(d, 'jars/j2/rounds/r1'), round());
    await assertFails(b.commit());
  });
});

describe('reading', () => {
  beforeEach(() => createJar('alice'));

  it('lets a member read the jar, its members and rounds, and list their jars', async () => {
    const d = db('alice');
    await assertSucceeds(getDoc(doc(d, 'jars/j1')));
    await assertSucceeds(getDocs(collection(d, 'jars/j1/members')));
    await assertSucceeds(getDoc(doc(d, 'jars/j1/rounds/r1')));
    await assertSucceeds(getDocs(query(collection(d, 'jars'), where('memberIds', 'array-contains', 'alice'))));
  });

  it('hides everything from non-members and signed-out callers', async () => {
    for (const d of [db('bob'), db(null)]) {
      await assertFails(getDoc(doc(d, 'jars/j1')));
      await assertFails(getDocs(collection(d, 'jars/j1/members')));
      await assertFails(getDoc(doc(d, 'jars/j1/rounds/r1')));
    }
    await assertFails(getDocs(collection(db('bob'), 'jars')));
    await assertFails(getDocs(query(collection(db('bob'), 'jars'), where('memberIds', 'array-contains', 'alice'))));
  });

  it('lets a signed-in user fetch a code they know, but not list codes', async () => {
    await assertSucceeds(getDoc(doc(db('bob'), `inviteCodes/${CODE}`)));
    await assertFails(getDoc(doc(db(null), `inviteCodes/${CODE}`)));
    await assertFails(getDocs(collection(db('bob'), 'inviteCodes')));
  });
});

describe('joining a jar', () => {
  beforeEach(() => createJar('alice'));

  it('works as the app does it, and then grants access', async () => {
    await assertSucceeds(joinJar('bob'));
    await assertSucceeds(getDoc(doc(db('bob'), 'jars/j1')));
    await assertSucceeds(getDocs(collection(db('bob'), 'jars/j1/members')));
  });

  it('needs the member document, with the right invite code', async () => {
    await assertFails(joinJar('bob', { member: null }));
    await assertFails(joinJar('bob', { member: member('MEMBER', { joinCode: 'ZZZ999' }) }));
  });

  it('cannot join with an elevated role or a head start', async () => {
    await assertFails(joinJar('bob', { member: member('ADMIN') }));
    await assertFails(joinJar('bob', { member: member('OWNER') }));
    await assertFails(joinJar('bob', { member: member('MEMBER', { paidMinor: 5000 }) }));
  });

  it('cannot add someone else, remove anyone, or change other fields while joining', async () => {
    await assertFails(joinJar('bob', { jar: { memberIds: ['alice', 'bob', 'carol'] } }));
    await assertFails(joinJar('bob', { jar: { memberIds: ['bob'] } }));
    await assertFails(joinJar('bob', { jar: { memberIds: arrayUnion('bob'), ownerId: 'bob' } }));
    await assertFails(joinJar('bob', { jar: { memberIds: arrayUnion('bob'), goalMinor: 1 } }));
    await assertFails(joinJar('bob', { jar: { memberIds: arrayUnion('bob'), roundTotalMinor: 0, status: 'CLOSED' } }));
  });

  it('cannot create a member document without joining', async () => {
    const d = db('bob');
    const b = writeBatch(d);
    b.set(doc(d, 'jars/j1/members/bob'), member('MEMBER'));
    await assertFails(b.commit());
  });

  it("cannot create someone else's member document", async () => {
    const d = db('bob');
    const b = writeBatch(d);
    b.update(doc(d, 'jars/j1'), { memberIds: arrayUnion('bob') });
    b.set(doc(d, 'jars/j1/members/bob'), member('MEMBER'));
    b.set(doc(d, 'jars/j1/members/carol'), member('MEMBER'));
    await assertFails(b.commit());
  });

  it('is refused once the jar is full', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      const ids = ['alice', ...Array.from({ length: 49 }, (_, i) => `u${i}`)];
      await updateDoc(doc(ctx.firestore(), 'jars/j1'), { memberIds: ids });
    });
    await assertFails(joinJar('bob'));
  });

  it('is refused on a closed jar', async () => {
    await env.withSecurityRulesDisabled((ctx) => updateDoc(doc(ctx.firestore(), 'jars/j1'), { status: 'CLOSED' }));
    await assertFails(joinJar('bob'));
  });
});

describe('after creation', () => {
  beforeEach(async () => {
    await createJar('alice');
    await joinJar('bob');
  });

  it('members cannot edit the jar, roles, balances, rounds or codes yet', async () => {
    for (const uid of ['alice', 'bob']) {
      const d = db(uid);
      await assertFails(updateDoc(doc(d, 'jars/j1'), { name: 'Renamed' }));
      await assertFails(updateDoc(doc(d, 'jars/j1'), { roundTotalMinor: 999 }));
      await assertFails(updateDoc(doc(d, 'jars/j1'), { ownerId: uid }));
      await assertFails(updateDoc(doc(d, `jars/j1/members/${uid}`), { role: 'ADMIN' }));
      await assertFails(updateDoc(doc(d, `jars/j1/members/${uid}`), { paidMinor: 100 }));
      await assertFails(updateDoc(doc(d, 'jars/j1/rounds/r1'), { totalMinor: 100 }));
      await assertFails(updateDoc(doc(d, `inviteCodes/${CODE}`), { jarId: 'other' }));
      await assertFails(deleteDoc(doc(d, 'jars/j1')));
      await assertFails(deleteDoc(doc(d, 'jars/j1/members/bob')));
      await assertFails(deleteDoc(doc(d, `inviteCodes/${CODE}`)));
    }
  });

  it('a member cannot join twice or add a second round', async () => {
    await assertFails(joinJar('bob'));
    const d = db('alice');
    const b = writeBatch(d);
    b.set(doc(d, 'jars/j1/rounds/r2'), round());
    await assertFails(b.commit());
  });
});
