package com.university.registration.model;

import com.university.registration.model.Course.Department;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for the Course ADT.
 *
 * Tests: construction validation, seat reservation, thread-safety,
 * prerequisite lists, equals/hashCode contract.
 *
 * Run with: java -cp out com.university.registration.model.CourseTest
 */
public class CourseTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(" CourseTest \n");

        test_construction_validCourse();
        test_construction_blankIdThrows();
        test_construction_zeroCreditThrows();
        test_construction_negativeCapacityThrows();
        test_reserveSeat_success();
        test_reserveSeat_whenFull_returnsFalse();
        test_releaseSeat_restoresSeat();
        test_reserveSeat_threadSafety();
        test_prerequisites_defensiveCopy();
        test_equals_sameCourseId();
        test_toString_containsCourseId();

        System.out.println("\n--- Results ---");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    //  Construction 

    static void test_construction_validCourse() {
        try {
            Course c = new Course("CPS101", "Intro", Department.COMPUTER_SCIENCE, 3, 30);
            assert_equals("CPS101", c.getCourseId(), "courseId should be uppercased");
            assert_equals(30, c.getMaxCapacity(), "capacity should be 30");
            assert_equals(0, c.getEnrolledCount(), "initial enrolled should be 0");
            pass("test_construction_validCourse");
        } catch (Exception e) { fail("test_construction_validCourse", e); }
    }

    static void test_construction_blankIdThrows() {
        try {
            new Course("", "Title", Department.MATHEMATICS, 3, 10);
            fail("test_construction_blankIdThrows", "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("test_construction_blankIdThrows");
        } catch (Exception e) { fail("test_construction_blankIdThrows", e); }
    }

    static void test_construction_zeroCreditThrows() {
        try {
            new Course("CPS101", "Title", Department.MATHEMATICS, 0, 10);
            fail("test_construction_zeroCreditThrows", "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("test_construction_zeroCreditThrows");
        } catch (Exception e) { fail("test_construction_zeroCreditThrows", e); }
    }

    static void test_construction_negativeCapacityThrows() {
        try {
            new Course("CPS101", "Title", Department.MATHEMATICS, 3, -5);
            fail("test_construction_negativeCapacityThrows", "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("test_construction_negativeCapacityThrows");
        } catch (Exception e) { fail("test_construction_negativeCapacityThrows", e); }
    }

    //  Seat management 

    static void test_reserveSeat_success() {
        try {
            Course c = new Course("CPS101", "Intro", Department.COMPUTER_SCIENCE, 3, 5);
            boolean reserved = c.reserveSeat();
            assert_true(reserved, "reserveSeat should return true when seats available");
            assert_equals(1, c.getEnrolledCount(), "enrolled count should be 1");
            assert_equals(4, c.getAvailableSeats(), "available seats should be 4");
            pass("test_reserveSeat_success");
        } catch (Exception e) { fail("test_reserveSeat_success", e); }
    }

    static void test_reserveSeat_whenFull_returnsFalse() {
        try {
            Course c = new Course("CPS101", "Intro", Department.COMPUTER_SCIENCE, 3, 2);
            c.reserveSeat();
            c.reserveSeat();
            boolean third = c.reserveSeat();
            assert_true(!third, "reserveSeat should return false when full");
            assert_equals(2, c.getEnrolledCount(), "enrolled count should stay at 2");
            pass("test_reserveSeat_whenFull_returnsFalse");
        } catch (Exception e) { fail("test_reserveSeat_whenFull_returnsFalse", e); }
    }

    static void test_releaseSeat_restoresSeat() {
        try {
            Course c = new Course("CPS101", "Intro", Department.COMPUTER_SCIENCE, 3, 2);
            c.reserveSeat();
            c.reserveSeat();
            assert_true(!c.hasAvailableSeats(), "should be full");
            c.releaseSeat();
            assert_true(c.hasAvailableSeats(), "seat should be available after release");
            assert_equals(1, c.getEnrolledCount(), "enrolled count should be 1");
            pass("test_releaseSeat_restoresSeat");
        } catch (Exception e) { fail("test_releaseSeat_restoresSeat", e); }
    }

    //  Concurrency stress test 

    static void test_reserveSeat_threadSafety() throws InterruptedException {
        try {
            int capacity = 10;
            int threads  = 50;
            Course c = new Course("CPS999", "Stress", Department.COMPUTER_SCIENCE, 3, capacity);

            List<Thread> workers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                workers.add(new Thread(c::reserveSeat));
            }

            long start = System.currentTimeMillis();
            workers.forEach(Thread::start);
            for (Thread t : workers) t.join();
            long duration = System.currentTimeMillis() - start;

            // Only 'capacity' seats should be filled — no race condition allowed
            assert_equals(capacity, c.getEnrolledCount(),
                "Thread safety: only " + capacity + " seats should be reserved out of " + threads + " attempts");
            assert_equals(0, c.getAvailableSeats(), "No seats should remain");

            System.out.println("  [STRESS] " + threads + " threads competed for "
                + capacity + " seats in " + duration + "ms — no race condition detected.");
            pass("test_reserveSeat_threadSafety");
        } catch (Exception e) { fail("test_reserveSeat_threadSafety", e); }
    }

    //  Defensive copy 

    static void test_prerequisites_defensiveCopy() {
        try {
            List<String> prereqs = new ArrayList<>(List.of("CPS101"));
            Course c = new Course("CPS201", "DS", Department.COMPUTER_SCIENCE, 3, 30, prereqs);
            prereqs.add("CPS999");   // mutate original list
            assert_equals(1, c.getPrerequisiteCourseIds().size(),
                "Prerequisite list should be defensively copied — external mutation must not affect course");
            pass("test_prerequisites_defensiveCopy");
        } catch (Exception e) { fail("test_prerequisites_defensiveCopy", e); }
    }

    // Object contract 

    static void test_equals_sameCourseId() {
        try {
            Course a = new Course("CPS101", "Intro A", Department.COMPUTER_SCIENCE, 3, 10);
            Course b = new Course("CPS101", "Intro B", Department.MATHEMATICS, 4, 20);
            assert_true(a.equals(b), "Courses with same ID should be equal");
            assert_true(a.hashCode() == b.hashCode(), "Equal objects must have same hashCode");
            pass("test_equals_sameCourseId");
        } catch (Exception e) { fail("test_equals_sameCourseId", e); }
    }

    static void test_toString_containsCourseId() {
        try {
            Course c = new Course("CPS314", "PL", Department.COMPUTER_SCIENCE, 3, 30);
            assert_true(c.toString().contains("CPS314"), "toString should contain courseId");
            pass("test_toString_containsCourseId");
        } catch (Exception e) { fail("test_toString_containsCourseId", e); }
    }

    // Test helpers 

    static void assert_equals(Object expected, Object actual, String message) {
        if (!expected.equals(actual))
            throw new AssertionError(message + " | expected=" + expected + " actual=" + actual);
    }

    static void assert_true(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void pass(String name) {
        System.out.println("  PASS  " + name);
        passed++;
    }

    static void fail(String name, Object detail) {
        System.out.println("  FAIL  " + name + " — " + detail);
        failed++;
    }
}
