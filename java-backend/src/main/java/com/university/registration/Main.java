package com.university.registration;

import com.university.registration.exception.*;
import com.university.registration.model.*;
import com.university.registration.model.Course.Department;
import com.university.registration.model.Student.AcademicLevel;
import com.university.registration.service.RegistrationService;

import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        RegistrationService service = new RegistrationService();

        System.out.println("Course Registration System \n");

        // 1. Courses
        service.addCourse(new Course("CPS101", "Intro to Programming",
            Department.COMPUTER_SCIENCE, 3, 30));
        service.addCourse(new Course("CPS201", "Data Structures",
            Department.COMPUTER_SCIENCE, 3, 30, List.of("CPS101")));
        service.addCourse(new Course("CPS314", "Programming Languages",
            Department.COMPUTER_SCIENCE, 3, 3, List.of("CPS101", "CPS201")));
        service.addCourse(new Course("MTH101", "Calculus I",
            Department.MATHEMATICS, 4, 50));

        // 2. Students
        service.addStudent(new Student("S001", "Sulayman kinteh",  "sulaymankinteh@gmail.com",
            AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
        service.addStudent(new Student("S002", "Buba Faye",    "buba@gmail.com",
            AcademicLevel.FRESHMAN));
        service.addStudent(new Student("S003", "Alieu jallow",  "alieu@gamil.com",
            AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
        service.addStudent(new Student("S004", "lamin Touray", "lamin@gmail.com",
            AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
        service.addStudent(new Student("S005", "Ebrima Ceesay",  "ebrima@gmail.com",
            AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));

        // 3. Synchronous enrollment
        System.out.println(" Synchronous Enrollment ");
        tryEnroll(service, "S001", "CPS314");
        tryEnroll(service, "S003", "CPS314");
        tryEnroll(service, "S001", "MTH101");

        // 4. Exception cases
        System.out.println("\n Exception Handling ");
        tryEnroll(service, "S002", "CPS314");   // prereq missing
        tryEnroll(service, "S004", "CPS314");   // last seat
        tryEnroll(service, "S005", "CPS314");   // course full

        // 5. Drop
        System.out.println("\n Drop Course ");
        tryDrop(service, "S001", "CPS314");
        System.out.println("  Seats in CPS314 after drop: " + getSeats(service, "CPS314"));

        // 6. Available courses
        System.out.println("\n Available Courses ");
        service.getAvailableCourses()
            .forEach(c -> System.out.println("  " + c));
        System.out.println("Total active enrollments: " + service.getTotalEnrollments());

        // 7. Async concurrent enrollment
        System.out.println("\n Async Concurrent Enrollment ");
        System.out.println("Submitting 5 requests simultaneously...");
        for (int i = 1; i <= 5; i++) {
            final String sid = "S00" + i;
            new Thread(() -> service.submitAsync(sid, "CPS101"), "producer-" + i).start();
        }
        Thread.sleep(1500);
        System.out.println("Queue drained.");
        System.out.println("Total active enrollments: " + service.getTotalEnrollments());

        service.shutdown();
        System.out.println("\n Done ");
    }

    private static void tryEnroll(RegistrationService svc, String sid, String cid) {
        try {
            svc.enroll(sid, cid);
            System.out.println("  ENROLLED : " + sid + " -> " + cid);
        } catch (CourseFullException e) {
            System.out.println("  FULL     : " + sid + " -> " + cid + " | " + e.getMessage());
        } catch (PrerequisiteNotMetException e) {
            System.out.println("  PREREQ   : " + sid + " -> " + cid + " | missing: " + e.getMissingPrerequisites());
        } catch (RegistrationException e) {
            System.out.println("  ERROR    : " + sid + " -> " + cid + " | " + e);
        }
    }

    private static void tryDrop(RegistrationService svc, String sid, String cid) {
        try {
            svc.drop(sid, cid);
            System.out.println("  DROPPED  : " + sid + " -> " + cid);
        } catch (RegistrationException e) {
            System.out.println("  DROP ERR : " + e.getMessage());
        }
    }

    private static int getSeats(RegistrationService svc, String cid) {
        try { return svc.getCourse(cid).getAvailableSeats(); }
        catch (Exception e) { return -1; }
    }
}
