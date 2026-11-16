import { after, before, beforeEach, describe, it } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, deleteField, doc, getDoc, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore';

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
const profile = (extra = {}) => ({
  displayName: 'Alice',
  createdAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
  ...extra,
});
const seed = () => assertSucceeds(setDoc(doc(db('alice'), 'users/alice'), profile()));

describe('users/{uid}', () => {
  it('lets a user create and read their own profile', async () => {
    await seed();
    await assertSucceeds(getDoc(doc(db('alice'), 'users/alice')));
  });

  it('accepts an https photo and rejects anything else', async () => {
    await assertSucceeds(setDoc(doc(db('alice'), 'users/alice'), profile({ photoUrl: 'https://example.com/a.png' })));
    await assertFails(setDoc(doc(db('bob'), 'users/bob'), profile({ photoUrl: 'javascript:alert(1)' })));
    await assertFails(setDoc(doc(db('bob'), 'users/bob'), profile({ photoUrl: 42 })));
  });

  it('hides profiles from other users and from signed-out callers', async () => {
    await seed();
    await assertFails(getDoc(doc(db('bob'), 'users/alice')));
    await assertFails(getDoc(doc(db(null), 'users/alice')));
  });

  it("stops a user writing someone else's profile", async () => {
    await assertFails(setDoc(doc(db('bob'), 'users/alice'), profile()));
    await seed();
    await assertFails(updateDoc(doc(db('bob'), 'users/alice'), { displayName: 'Hacked', updatedAt: serverTimestamp() }));
  });

  it('rejects unknown fields, bad names and client-chosen timestamps', async () => {
    const ref = doc(db('alice'), 'users/alice');
    await assertFails(setDoc(ref, profile({ role: 'admin' })));
    await assertFails(setDoc(ref, profile({ email: 'a@example.com' })));
    await assertFails(setDoc(ref, profile({ displayName: '' })));
    await assertFails(setDoc(ref, profile({ displayName: 'x'.repeat(81) })));
    await assertFails(setDoc(ref, profile({ displayName: 7 })));
    await assertFails(setDoc(ref, profile({ createdAt: new Date(0) })));
    await assertFails(setDoc(ref, { displayName: 'Alice' }));
  });

  it('allows the updates the app makes', async () => {
    await seed();
    const ref = doc(db('alice'), 'users/alice');
    await assertSucceeds(updateDoc(ref, { displayName: 'Alice B', photoUrl: 'https://example.com/b.png', updatedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(ref, { displayName: 'Alice B', photoUrl: deleteField(), updatedAt: serverTimestamp() }));
  });

  it('keeps createdAt fixed and requires a fresh updatedAt', async () => {
    await seed();
    const ref = doc(db('alice'), 'users/alice');
    await assertFails(updateDoc(ref, { createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(ref, { displayName: 'Alice B' }));
    await assertFails(updateDoc(ref, { displayName: deleteField(), updatedAt: serverTimestamp() }));
  });

  it('does not allow deleting a profile', async () => {
    await seed();
    await assertFails(deleteDoc(doc(db('alice'), 'users/alice')));
  });
});

describe('everything else', () => {
  it('is denied until the app writes it', async () => {
    await assertFails(getDoc(doc(db('alice'), 'jars/j1')));
    await assertFails(setDoc(doc(db('alice'), 'jars/j1'), { name: 'Jar', ownerId: 'alice' }));
    await assertFails(setDoc(doc(db('alice'), 'inviteCodes/ABC234'), { jarId: 'j1' }));
  });
});
