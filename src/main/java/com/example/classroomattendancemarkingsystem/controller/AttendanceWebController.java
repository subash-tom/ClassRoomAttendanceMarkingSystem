package com.example.classroomattendancemarkingsystem.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.classroomattendancemarkingsystem.model.AttendanceRecord;
import com.example.classroomattendancemarkingsystem.model.Session;
import com.example.classroomattendancemarkingsystem.model.Student;
import com.example.classroomattendancemarkingsystem.repository.AttendanceRecordRepository;
import com.example.classroomattendancemarkingsystem.repository.SessionRepository;
import com.example.classroomattendancemarkingsystem.repository.StudentRepository;

@Controller
@RequestMapping("/index")
public class AttendanceWebController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRepository studentRepository;
    private final SessionRepository sessionRepository;

    public AttendanceWebController(
            AttendanceRecordRepository attendanceRecordRepository,
            StudentRepository studentRepository,
            SessionRepository sessionRepository) {

        this.attendanceRecordRepository = attendanceRecordRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
    }

    // =========================================================
    // ATTENDANCE PAGE
    // GET /attendance
    // =========================================================

    @GetMapping
    public String attendancePage(Model model) {

        List<Student> students = studentRepository.findAll();

        List<Session> sessions = sessionRepository.findAll();

        model.addAttribute("students", students);
        model.addAttribute("sessions", sessions);

        return "index";
    }

    // =========================================================
    // SAVE ATTENDANCE
    // POST /attendance/save
    // =========================================================

    @PostMapping("/save")
    public String saveAttendance(
            @RequestParam("sessionId") Long sessionId,
            @RequestParam(value = "attendanceDate", required = false)
            String attendanceDate,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {

        try {

            Session selectedSession = sessionRepository
                    .findById(sessionId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Session not found: " + sessionId
                            )
                    );

            List<Student> students =
                    studentRepository.findAll();

            int savedCount = 0;
            int skippedCount = 0;

            for (Student student : students) {

                String key =
                        "attendance_" + student.getStudentId();

                String status = params.get(key);

                if (status == null || status.isBlank()) {
                    continue;
                }

                boolean present =
                        "PRESENT".equalsIgnoreCase(status);

                boolean alreadyExists =
                        attendanceRecordRepository
                                .existsByStudentAndSession(
                                        student,
                                        selectedSession
                                );

                if (alreadyExists) {
                    skippedCount++;
                    continue;
                }

                AttendanceRecord attendance =
                        new AttendanceRecord();

                attendance.setStudent(student);
                attendance.setSession(selectedSession);
                attendance.setPresent(present);

                attendanceRecordRepository.save(attendance);

                savedCount++;
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Attendance saved successfully. "
                            + "Saved: " + savedCount
                            + ", Already marked: " + skippedCount
            );

            return "redirect:/index/view";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to save attendance: "
                            + e.getMessage()
            );

            return "redirect:/index/view";
        }
    }

    // =========================================================
    // VIEW ATTENDANCE
    // GET /attendance/view
    // =========================================================

    @GetMapping("/view")
    public String viewAttendance(Model model) {

        List<AttendanceRecord> attendanceRecords =
                attendanceRecordRepository.findAll();

        model.addAttribute(
                "attendanceRecords",
                attendanceRecords
        );

        return "index-view";
    }

    // =========================================================
    // DELETE ATTENDANCE
    // GET /attendance/delete/{id}
    // =========================================================

    @GetMapping("/delete/{id}")
    public String deleteAttendance(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {

            if (!attendanceRecordRepository.existsById(id)) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Attendance record not found: " + id
                );

                return "redirect:/index/view";
            }

            attendanceRecordRepository.deleteById(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Attendance deleted successfully."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to delete attendance: "
                            + e.getMessage()
            );
        }

        return "redirect:/index/view";
    }
}