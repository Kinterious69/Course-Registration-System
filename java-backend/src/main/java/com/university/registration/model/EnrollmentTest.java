package com.university.registration.model;

/**
 * Unit tests for the Enrollment ADT.
 *
 * Tests: construction, state transitions, illegal transitions,
 * immutability of identity fields.
 *
 * Run with: java -cp out com.university.registration.model.EnrollmentTest
 */
public class EnrollmentTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println(" EnrollmentTest \n");

        test_construction_initialStatusActive();
        test_construction_uniqueIds();
        test_drop_fromActive_succeeds();
        test_complete_fromActive_succeeds();
        test_drop_fromDropped_throws();
        test_complete_fromDropped_throws();
        test_drop_fromCompleted_throws();
        test_complete_fromCompleted_throws();
        test_isActive_afterDrop_false();

        System.out.println("\n--- Results ---");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    static void test_construction_initialStatusActive() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            assert_equals(Enrollment.Status.ACTIVE, e.getStatus(), "Initial status should be ACTIVE");
            assert_true(e.isActive(), "isActive() should return true initially");
            assert_true(e.getEnrollmentId() != null, "enrollmentId should not be null");
            assert_true(e.getEnrolledAt() != null, "enrolledAt should not be null");
            pass("test_construction_initialStatusActive");
        } catch (Exception e) { fail("test_construction_initialStatusActive", e); }
    }

    static void test_construction_uniqueIds() {
        try {
            Enrollment a = new Enrollment("S001", "CPS314");
            Enrollment b = new Enrollment("S001", "CPS314");
            assert_true(!a.getEnrollmentId().equals(b.getEnrollmentId()),
                "Each enrollment should have a unique ID (UUID)");
            pass("test_construction_uniqueIds");
        } catch (Exception e) { fail("test_construction_uniqueIds", e); }
    }

    static void test_drop_fromActive_succeeds() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            e.drop();
            assert_equals(Enrollment.Status.DROPPED, e.getStatus(), "Status should be DROPPED");
            assert_true(!e.isActive(), "isActive should be false after drop");
            pass("test_drop_fromActive_succeeds");
        } catch (Exception e) { fail("test_drop_fromActive_succeeds", e); }
    }

    static void test_complete_fromActive_succeeds() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            e.complete();
            assert_equals(Enrollment.Status.COMPLETED, e.getStatus(), "Status should be COMPLETED");
            assert_true(!e.isActive(), "isActive should be false after complete");
            pass("test_complete_fromActive_succeeds");
        } catch (Exception e) { fail("test_complete_fromActive_succeeds", e); }
    }

    static void test_drop_fromDropped_throws() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            e.drop();
            e.drop();   // second drop should throw
            fail("test_drop_fromDropped_throws", "Expected IllegalStateException");
        } catch (IllegalStateException e) {
            pass("test_drop_fromDropped_throws");
        } catch (Exception e) { fail("test_drop_fromDropped_throws", e); }
    }

    static void test_complete_fromDropped_throws() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            e.drop();
            e.complete();   // can't complete a dropped enrollment
            fail("test_complete_fromDropped_throws", "Expected IllegalStateException");
        } catch (IllegalStateException e) {
            pass("test_complete_fromDropped_throws");
        } catch (Exception e) { fail("test_complete_fromDropped_throws", e); }
    }

    static void test_drop_fromCompleted_throws() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            e.complete();
            e.drop();   // can't drop a completed enrollment
            fail("test_drop_fromCompleted_throws", "Expected IllegalStateException");
        } catch (IllegalStateException e) {
            pass("test_drop_fromCompleted_throws");
        } catch (Exception e) { fail("test_drop_fromCompleted_throws", e); }
    }

    static void test_complete_fromCompleted_throws() {
        try {
            Enrollment e = new Enrollment("S001", "CPS314");
            e.complete();
            e.complete();   // can't complete twice
            fail("test_complete_fromCompleted_throws", "Expected IllegalStateException");
        } catch (IllegalStateException e) {
            pass("test_complete_fromCompleted_throws");
        } catch (Exception e) { fail("test_complete_fromCompleted_throws", e); }
    }

    static void test_isActive_afterDrop_false() {
        try {
            Enrollment e = new Enrollment("S002", "MTH101");
            assert_true(e.isActive(), "Should be active initially");
            e.drop();
            assert_true(!e.isActive(), "Should not be active after drop");
            pass("test_isActive_afterDrop_false");
        } catch (Exception e) { fail("test_isActive_afterDrop_false", e); }
    }

    //  Helpers

    static void assert_equals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual))
            throw new AssertionError(msg + " | expected=" + expected + " actual=" + actual);
    }

    static void assert_true(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    static void pass(String name) { System.out.println("  PASS  " + name); passed++; }
    static void fail(String name, Object detail) {
        System.out.println("  FAIL  " + name + " — " + detail); failed++;
    }
}
