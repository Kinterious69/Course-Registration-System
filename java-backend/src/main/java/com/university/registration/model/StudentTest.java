package com.university.registration.model;

import com.university.registration.model.Student.AcademicLevel;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for the Student ADT.
 *
 * Tests: construction, prerequisite checking, completed course tracking,
 * defensive copying, equals/hashCode contract.
 *
 * Run with: java -cp out com.university.registration.model.StudentTest
 */
public class StudentTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== StudentTest ===\n");

        test_construction_valid();
        test_construction_blankIdThrows();
        test_construction_invalidEmailThrows();
        test_construction_idUppercased();
        test_findMissingPrerequisites_allMet();
        test_findMissingPrerequisites_someMissing();
        test_findMissingPrerequisites_caseInsensitive();
        test_addCompletedCourse_noDuplicates();
        test_completedCourses_defensiveCopy();
        test_addCreditHours_negativeThrows();
        test_equals_sameStudentId();
        test_hasCompleted_caseInsensitive();

        System.out.println("\n--- Results ---");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // Construction 

    static void test_construction_valid() {
        try {
            Student s = new Student("S001", "Alice", "alice@uni.edu", AcademicLevel.SOPHOMORE);
            assert_equals("S001", s.getStudentId(), "studentId");
            assert_equals("alice@uni.edu", s.getEmail(), "email lowercased");
            assert_equals(0, s.getTotalCreditHours(), "initial credits = 0");
            assert_equals(0, s.getCompletedCourseIds().size(), "no completed courses initially");
            pass("test_construction_valid");
        } catch (Exception e) { fail("test_construction_valid", e); }
    }

    static void test_construction_blankIdThrows() {
        try {
            new Student("", "Alice", "alice@uni.edu", AcademicLevel.FRESHMAN);
            fail("test_construction_blankIdThrows", "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("test_construction_blankIdThrows");
        } catch (Exception e) { fail("test_construction_blankIdThrows", e); }
    }

    static void test_construction_invalidEmailThrows() {
        try {
            new Student("S001", "Alice", "notanemail", AcademicLevel.FRESHMAN);
            fail("test_construction_invalidEmailThrows", "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("test_construction_invalidEmailThrows");
        } catch (Exception e) { fail("test_construction_invalidEmailThrows", e); }
    }

    static void test_construction_idUppercased() {
        try {
            Student s = new Student("s001", "Alice", "alice@uni.edu", AcademicLevel.FRESHMAN);
            assert_equals("S001", s.getStudentId(), "ID should be uppercased");
            pass("test_construction_idUppercased");
        } catch (Exception e) { fail("test_construction_idUppercased", e); }
    }

    // Prerequisite checking 

    static void test_findMissingPrerequisites_allMet() {
        try {
            Student s = new Student("S001", "Alice", "alice@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201"));
            List<String> missing = s.findMissingPrerequisites(List.of("CPS101", "CPS201"));
            assert_equals(0, missing.size(), "No prerequisites should be missing");
            pass("test_findMissingPrerequisites_allMet");
        } catch (Exception e) { fail("test_findMissingPrerequisites_allMet", e); }
    }

    static void test_findMissingPrerequisites_someMissing() {
        try {
            Student s = new Student("S002", "Bob", "bob@uni.edu",
                AcademicLevel.FRESHMAN, List.of("CPS101"));
            List<String> missing = s.findMissingPrerequisites(List.of("CPS101", "CPS201"));
            assert_equals(1, missing.size(), "Should have 1 missing prerequisite");
            assert_true(missing.contains("CPS201"), "CPS201 should be missing");
            pass("test_findMissingPrerequisites_someMissing");
        } catch (Exception e) { fail("test_findMissingPrerequisites_someMissing", e); }
    }

    static void test_findMissingPrerequisites_caseInsensitive() {
        try {
            Student s = new Student("S001", "Alice", "alice@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("cps101"));   // lowercase
            List<String> missing = s.findMissingPrerequisites(List.of("CPS101")); // uppercase
            assert_equals(0, missing.size(), "Prereq check should be case-insensitive");
            pass("test_findMissingPrerequisites_caseInsensitive");
        } catch (Exception e) { fail("test_findMissingPrerequisites_caseInsensitive", e); }
    }

    // Completed courses

    static void test_addCompletedCourse_noDuplicates() {
        try {
            Student s = new Student("S001", "Alice", "alice@uni.edu", AcademicLevel.SOPHOMORE);
            s.addCompletedCourse("CPS101");
            s.addCompletedCourse("CPS101");   // duplicate
            assert_equals(1, s.getCompletedCourseIds().size(), "No duplicate completed courses");
            pass("test_addCompletedCourse_noDuplicates");
        } catch (Exception e) { fail("test_addCompletedCourse_noDuplicates", e); }
    }

    static void test_completedCourses_defensiveCopy() {
        try {
            List<String> completed = new ArrayList<>(List.of("CPS101"));
            Student s = new Student("S001", "Alice", "alice@uni.edu",
                AcademicLevel.SOPHOMORE, completed);
            completed.add("CPS999");   // mutate original
            assert_equals(1, s.getCompletedCourseIds().size(),
                "Completed courses should be defensively copied");
            pass("test_completedCourses_defensiveCopy");
        } catch (Exception e) { fail("test_completedCourses_defensiveCopy", e); }
    }

    static void test_addCreditHours_negativeThrows() {
        try {
            Student s = new Student("S001", "Alice", "alice@uni.edu", AcademicLevel.FRESHMAN);
            s.addCreditHours(-3);
            fail("test_addCreditHours_negativeThrows", "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("test_addCreditHours_negativeThrows");
        } catch (Exception e) { fail("test_addCreditHours_negativeThrows", e); }
    }

    //  Object contract 

    static void test_equals_sameStudentId() {
        try {
            Student a = new Student("S001", "Alice", "alice@uni.edu", AcademicLevel.FRESHMAN);
            Student b = new Student("S001", "Different Name", "other@uni.edu", AcademicLevel.SENIOR);
            assert_true(a.equals(b), "Students with same ID should be equal");
            assert_true(a.hashCode() == b.hashCode(), "Equal objects must have same hashCode");
            pass("test_equals_sameStudentId");
        } catch (Exception e) { fail("test_equals_sameStudentId", e); }
    }

    static void test_hasCompleted_caseInsensitive() {
        try {
            Student s = new Student("S001", "Alice", "alice@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("CPS101"));
            assert_true(s.hasCompleted("cps101"), "hasCompleted should be case-insensitive");
            assert_true(s.hasCompleted("CPS101"), "hasCompleted uppercase");
            assert_true(!s.hasCompleted("CPS999"), "hasCompleted should return false for unknown");
            pass("test_hasCompleted_caseInsensitive");
        } catch (Exception e) { fail("test_hasCompleted_caseInsensitive", e); }
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
