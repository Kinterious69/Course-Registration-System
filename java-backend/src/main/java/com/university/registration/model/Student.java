package com.university.registration.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ADT: Student
 *
 * Represents a registered university student.
 * Tracks identity, academic level, and the set of completed course IDs
 * (used for prerequisite checking).
 *
 * Demonstrates: encapsulation, defensive copying, immutable identity fields.
 */
public class Student implements StudentRegistry.Registrable {

    public enum AcademicLevel { FRESHMAN, SOPHOMORE, JUNIOR, SENIOR, POSTGRADUATE }

    // Identity (immutable after construction)
    private final String studentId;
    private final String fullName;
    private final String email;
    private final AcademicLevel level;

    //  Academic record (mutable) 
    private final List<String> completedCourseIds;   // prerequisite history
    private int totalCreditHours;

    //  Constructor 

    public Student(String studentId, String fullName, String email,
                   AcademicLevel level, List<String> completedCourseIds) {
        if (studentId == null || studentId.isBlank())
            throw new IllegalArgumentException("studentId must not be blank.");
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Invalid email address.");

        this.studentId   = studentId.toUpperCase();
        this.fullName    = Objects.requireNonNull(fullName, "fullName must not be null");
        this.email       = email.toLowerCase();
        this.level       = Objects.requireNonNull(level, "level must not be null");
        this.completedCourseIds = completedCourseIds == null
            ? new ArrayList<>()
            : new ArrayList<>(completedCourseIds);   // defensive copy
        this.totalCreditHours = 0;
    }

    /** Convenience constructor — no prior courses */
    public Student(String studentId, String fullName, String email, AcademicLevel level) {
        this(studentId, fullName, email, level, List.of());
    }

    //  Academic record mutation 
    /**
     * Records a completed course in this student's history.
     * Called after successful enrollment and course completion.
     */
    public void addCompletedCourse(String courseId) {
        Objects.requireNonNull(courseId, "courseId must not be null");
        if (!completedCourseIds.contains(courseId.toUpperCase())) {
            completedCourseIds.add(courseId.toUpperCase());
        }
    }

    public void addCreditHours(int hours) {
        if (hours < 0) throw new IllegalArgumentException("Credit hours cannot be negative.");
        this.totalCreditHours += hours;
    }

    //  Prerequisite checking 

    /**
     * Checks whether this student has completed all the given prerequisite course IDs.
     * Returns a list of any missing ones (empty list = all prerequisites satisfied).
     */
    public List<String> findMissingPrerequisites(List<String> requiredCourseIds) {
        List<String> missing = new ArrayList<>();
        for (String req : requiredCourseIds) {
            if (!completedCourseIds.contains(req.toUpperCase())) {
                missing.add(req.toUpperCase());
            }
        }
        return missing;
    }

    public boolean hasCompleted(String courseId) {
        return completedCourseIds.contains(courseId.toUpperCase());
    }

    // Getters 

    @Override
    public String getId()             { return studentId; }
    public String getStudentId()      { return studentId; }
    public String getFullName()       { return fullName; }
    public String getEmail()          { return email; }
    public AcademicLevel getLevel()   { return level; }
    public int getTotalCreditHours()  { return totalCreditHours; }

    public List<String> getCompletedCourseIds() {
        return Collections.unmodifiableList(completedCourseIds);
    }

    //  Object contract

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return studentId.equals(((Student) o).studentId);
    }

    @Override
    public int hashCode() { return studentId.hashCode(); }

    @Override
    public String toString() {
        return String.format("Student{id='%s', name='%s', level=%s, credits=%d}",
            studentId, fullName, level, totalCreditHours);
    }
}
