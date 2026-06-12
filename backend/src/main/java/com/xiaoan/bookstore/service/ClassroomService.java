package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private static final Logger log = LoggerFactory.getLogger(ClassroomService.class);
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;

    private final ClassroomMapper classroomMapper;
    private final ClassroomMemberMapper classroomMemberMapper;
    private final ClassroomAssignmentMapper classroomAssignmentMapper;
    private final ClassroomSubmissionMapper classroomSubmissionMapper;
    private final ClassroomReminderMapper classroomReminderMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    private String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateInviteCode();
        } while (classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getInviteCode, code)) > 0);
        return code;
    }

    private ClassroomMember getMemberOrThrow(Long classroomId, Long userId) {
        ClassroomMember member = classroomMemberMapper.selectByClassroomAndUser(classroomId, userId);
        if (member == null) {
            throw new BusinessException("您不是该班级成员");
        }
        return member;
    }

    private void validateTeacherRole(ClassroomMember member) {
        if (member.getRole() != 1) {
            throw new BusinessException("只有教师可以执行此操作");
        }
    }

    @Transactional
    public Classroom createClassroom(Long userId, ClassroomCreateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Classroom classroom = new Classroom();
        classroom.setName(dto.getName());
        classroom.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        classroom.setTeacherId(userId);
        classroom.setInviteCode(generateUniqueInviteCode());
        classroom.setMemberCount(1);
        classroom.setInstitution(dto.getInstitution() != null ? dto.getInstitution() : "");
        classroom.setGradeLevel(dto.getGradeLevel() != null ? dto.getGradeLevel() : "");
        classroom.setStatus(1);
        classroomMapper.insert(classroom);

        ClassroomMember member = new ClassroomMember();
        member.setClassroomId(classroom.getId());
        member.setUserId(userId);
        member.setRole(1);
        member.setStudentNo("");
        member.setRealName(user.getNickname());
        member.setJoinedAt(LocalDateTime.now());
        classroomMemberMapper.insert(member);

        log.info("创建班级: userId={}, classroomId={}, name={}", userId, classroom.getId(), classroom.getName());
        return classroom;
    }

    @Transactional
    public ClassroomMember joinClassroom(Long userId, ClassroomJoinDTO dto) {
        Classroom classroom = classroomMapper.selectOne(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getInviteCode, dto.getInviteCode()));
        if (classroom == null) {
            throw new BusinessException("邀请码无效");
        }
        if (classroom.getStatus() == 0) {
            throw new BusinessException("该班级已关闭");
        }

        Long count = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroom.getId())
                .eq(ClassroomMember::getUserId, userId));
        if (count > 0) {
            throw new BusinessException("您已加入该班级");
        }

        ClassroomMember member = new ClassroomMember();
        member.setClassroomId(classroom.getId());
        member.setUserId(userId);
        member.setRole(2);
        member.setStudentNo(dto.getStudentNo() != null ? dto.getStudentNo() : "");
        member.setRealName(dto.getRealName() != null ? dto.getRealName() : "");
        member.setJoinedAt(LocalDateTime.now());
        classroomMemberMapper.insert(member);

        classroom.setMemberCount(classroom.getMemberCount() + 1);
        classroomMapper.updateById(classroom);

        log.info("加入班级: userId={}, classroomId={}", userId, classroom.getId());
        return member;
    }

    @Transactional
    public void leaveClassroom(Long userId, Long classroomId) {
        ClassroomMember member = getMemberOrThrow(classroomId, userId);
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException("班级不存在");
        }

        if (member.getRole() == 1) {
            throw new BusinessException("教师不能退出班级，请先转让或关闭班级");
        }

        classroomMemberMapper.deleteById(member.getId());
        classroom.setMemberCount(Math.max(0, classroom.getMemberCount() - 1));
        classroomMapper.updateById(classroom);

        log.info("退出班级: userId={}, classroomId={}", userId, classroomId);
    }

    public List<Classroom> myClassrooms(Long userId) {
        return classroomMapper.selectByUserId(userId);
    }

    public ClassroomVO getClassroomDetail(Long userId, Long classroomId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException("班级不存在");
        }

        ClassroomMember myMember = classroomMemberMapper.selectByClassroomAndUser(classroomId, userId);

        User teacher = userMapper.selectById(classroom.getTeacherId());

        ClassroomVO vo = new ClassroomVO();
        vo.setId(classroom.getId());
        vo.setName(classroom.getName());
        vo.setDescription(classroom.getDescription());
        vo.setTeacherId(classroom.getTeacherId());
        vo.setTeacherNickname(teacher != null ? teacher.getNickname() : "");
        vo.setTeacherAvatar(teacher != null ? teacher.getAvatar() : "");
        vo.setInviteCode(myMember != null && myMember.getRole() == 1 ? classroom.getInviteCode() : null);
        vo.setMemberCount(classroom.getMemberCount());
        vo.setInstitution(classroom.getInstitution());
        vo.setGradeLevel(classroom.getGradeLevel());
        vo.setStatus(classroom.getStatus());
        vo.setMyRole(myMember != null ? myMember.getRole() : null);
        vo.setCreatedAt(classroom.getCreatedAt());

        if (myMember != null) {
            List<Map<String, Object>> memberMaps = classroomMemberMapper.selectMembersWithUserInfo(classroomId);
            List<ClassroomMemberVO> memberVOS = memberMaps.stream().map(m -> {
                ClassroomMemberVO mvo = new ClassroomMemberVO();
                mvo.setUserId(((Number) m.get("user_id")).longValue());
                mvo.setNickname((String) m.get("nickname"));
                mvo.setAvatar((String) m.get("avatar"));
                mvo.setRole(((Number) m.get("role")).intValue());
                mvo.setStudentNo((String) m.get("student_no"));
                mvo.setRealName((String) m.get("real_name"));
                mvo.setJoinedAt((LocalDateTime) m.get("joined_at"));
                return mvo;
            }).collect(Collectors.toList());
            vo.setMembers(memberVOS);
        }

        return vo;
    }

    @Transactional
    public ClassroomAssignment createAssignment(Long userId, Long classroomId, AssignmentCreateDTO dto) {
        ClassroomMember member = getMemberOrThrow(classroomId, userId);
        validateTeacherRole(member);

        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException("班级不存在");
        }
        if (classroom.getStatus() == 0) {
            throw new BusinessException("班级已关闭，无法布置作业");
        }

        if (dto.getStartPage() < 0 || dto.getEndPage() < dto.getStartPage()) {
            throw new BusinessException("页码范围无效");
        }

        LocalDateTime deadline = LocalDateTime.parse(dto.getDeadline());
        if (deadline.isBefore(LocalDateTime.now())) {
            throw new BusinessException("截止日期不能早于当前时间");
        }

        ClassroomAssignment assignment = new ClassroomAssignment();
        assignment.setClassroomId(classroomId);
        assignment.setTeacherId(userId);
        assignment.setBookId(dto.getBookId());
        assignment.setBookTitle(dto.getBookTitle());
        assignment.setBookAuthor(dto.getBookAuthor() != null ? dto.getBookAuthor() : "");
        assignment.setStartPage(dto.getStartPage());
        assignment.setEndPage(dto.getEndPage());
        assignment.setDeadline(deadline);
        assignment.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        assignment.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100);
        assignment.setStatus(1);
        assignment.setSubmitCount(0);
        assignment.setGradedCount(0);
        classroomAssignmentMapper.insert(assignment);

        List<Map<String, Object>> members = classroomMemberMapper.selectMembersWithUserInfo(classroomId);
        for (Map<String, Object> m : members) {
            if (((Number) m.get("role")).intValue() == 2) {
                Long studentId = ((Number) m.get("user_id")).longValue();
                Map<String, String> vars = new HashMap<>();
                vars.put("bookTitle", dto.getBookTitle());
                vars.put("className", classroom.getName());
                notificationService.sendByTemplate("classroom_new_assignment", studentId, vars,
                        "{\"type\":\"classroom_assignment\",\"classroomId\":" + classroomId + ",\"assignmentId\":" + assignment.getId() + "}");
            }
        }

        log.info("布置作业: teacherId={}, classroomId={}, assignmentId={}", userId, classroomId, assignment.getId());
        return assignment;
    }

    public List<AssignmentVO> listAssignments(Long userId, Long classroomId) {
        ClassroomMember member = getMemberOrThrow(classroomId, userId);
        Classroom classroom = classroomMapper.selectById(classroomId);

        List<ClassroomAssignment> assignments = classroomAssignmentMapper.selectByClassroomId(classroomId);

        long totalStudents = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getRole, 2));

        return assignments.stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            vo.setId(a.getId());
            vo.setClassroomId(a.getClassroomId());
            vo.setTeacherId(a.getTeacherId());
            User teacher = userMapper.selectById(a.getTeacherId());
            vo.setTeacherNickname(teacher != null ? teacher.getNickname() : "");
            vo.setBookId(a.getBookId());
            vo.setBookTitle(a.getBookTitle());
            vo.setBookAuthor(a.getBookAuthor());
            vo.setStartPage(a.getStartPage());
            vo.setEndPage(a.getEndPage());
            vo.setDeadline(a.getDeadline());
            vo.setDescription(a.getDescription());
            vo.setTotalScore(a.getTotalScore());
            vo.setStatus(a.getStatus());
            vo.setSubmitCount(a.getSubmitCount());
            vo.setGradedCount(a.getGradedCount());
            vo.setTotalMembers((int) totalStudents);
            vo.setAvgScore(classroomSubmissionMapper.selectAvgScoreByAssignment(a.getId()));
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    public AssignmentVO getAssignmentDetail(Long userId, Long assignmentId) {
        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }

        ClassroomMember member = getMemberOrThrow(assignment.getClassroomId(), userId);

        long totalStudents = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, assignment.getClassroomId())
                .eq(ClassroomMember::getRole, 2));

        User teacher = userMapper.selectById(assignment.getTeacherId());

        AssignmentVO vo = new AssignmentVO();
        vo.setId(assignment.getId());
        vo.setClassroomId(assignment.getClassroomId());
        vo.setTeacherId(assignment.getTeacherId());
        vo.setTeacherNickname(teacher != null ? teacher.getNickname() : "");
        vo.setBookId(assignment.getBookId());
        vo.setBookTitle(assignment.getBookTitle());
        vo.setBookAuthor(assignment.getBookAuthor());
        vo.setStartPage(assignment.getStartPage());
        vo.setEndPage(assignment.getEndPage());
        vo.setDeadline(assignment.getDeadline());
        vo.setDescription(assignment.getDescription());
        vo.setTotalScore(assignment.getTotalScore());
        vo.setStatus(assignment.getStatus());
        vo.setSubmitCount(assignment.getSubmitCount());
        vo.setGradedCount(assignment.getGradedCount());
        vo.setTotalMembers((int) totalStudents);
        vo.setAvgScore(classroomSubmissionMapper.selectAvgScoreByAssignment(assignmentId));
        vo.setCreatedAt(assignment.getCreatedAt());

        return vo;
    }

    @Transactional
    public ClassroomSubmission submitAssignment(Long userId, Long assignmentId, SubmissionDTO dto) {
        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        if (assignment.getStatus() == 2) {
            throw new BusinessException("作业已截止，无法提交");
        }
        if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
            assignment.setStatus(2);
            classroomAssignmentMapper.updateById(assignment);
            throw new BusinessException("已过截止日期，无法提交");
        }

        ClassroomMember member = getMemberOrThrow(assignment.getClassroomId(), userId);
        if (member.getRole() != 2) {
            throw new BusinessException("教师无需提交作业");
        }

        ClassroomSubmission existing = classroomSubmissionMapper.selectByAssignmentAndStudent(assignmentId, userId);
        if (existing != null && existing.getStatus() == 2) {
            throw new BusinessException("作业已批改，无法重新提交");
        }

        ClassroomSubmission submission;
        if (existing != null) {
            existing.setReadingDuration(dto.getReadingDuration());
            existing.setAnnotationSummary(dto.getAnnotationSummary() != null ? dto.getAnnotationSummary() : "");
            existing.setPageProgress(dto.getPageProgress() != null ? dto.getPageProgress() : 0);
            existing.setProofImages(dto.getProofImages() != null ? dto.getProofImages() : "");
            existing.setSubmitAt(LocalDateTime.now());
            existing.setStatus(1);
            classroomSubmissionMapper.updateById(existing);
            submission = existing;
        } else {
            submission = new ClassroomSubmission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(userId);
            submission.setReadingDuration(dto.getReadingDuration());
            submission.setAnnotationSummary(dto.getAnnotationSummary() != null ? dto.getAnnotationSummary() : "");
            submission.setPageProgress(dto.getPageProgress() != null ? dto.getPageProgress() : 0);
            submission.setProofImages(dto.getProofImages() != null ? dto.getProofImages() : "");
            submission.setSubmitAt(LocalDateTime.now());
            submission.setStatus(1);
            classroomSubmissionMapper.insert(submission);

            assignment.setSubmitCount(assignment.getSubmitCount() + 1);
            classroomAssignmentMapper.updateById(assignment);
        }

        log.info("提交作业: studentId={}, assignmentId={}", userId, assignmentId);
        return submission;
    }

    public IPage<SubmissionVO> listSubmissions(Long userId, Long assignmentId, int page, int size) {
        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }

        ClassroomMember member = getMemberOrThrow(assignment.getClassroomId(), userId);

        Page<ClassroomSubmission> pageParam = new Page<>(page, size);
        IPage<Map<String, Object>> pageResult = classroomSubmissionMapper.selectByAssignmentIdWithUserInfo(pageParam, assignmentId);

        List<SubmissionVO> vos = pageResult.getRecords().stream().map(m -> {
            SubmissionVO vo = new SubmissionVO();
            vo.setId(((Number) m.get("id")).longValue());
            vo.setAssignmentId(((Number) m.get("assignment_id")).longValue());
            vo.setStudentId(((Number) m.get("student_id")).longValue());
            vo.setStudentNickname((String) m.get("nickname"));
            vo.setStudentAvatar((String) m.get("avatar"));
            vo.setReadingDuration(m.get("reading_duration") != null ? ((Number) m.get("reading_duration")).intValue() : 0);
            vo.setAnnotationSummary((String) m.get("annotation_summary"));
            vo.setPageProgress(m.get("page_progress") != null ? ((Number) m.get("page_progress")).intValue() : 0);
            vo.setProofImages((String) m.get("proof_images"));
            vo.setSubmitAt((LocalDateTime) m.get("submit_at"));
            vo.setStatus(m.get("status") != null ? ((Number) m.get("status")).intValue() : 0);
            vo.setScore(m.get("score") != null ? ((Number) m.get("score")).intValue() : null);
            vo.setTeacherComment((String) m.get("teacher_comment"));
            vo.setGradedAt((LocalDateTime) m.get("graded_at"));
            if (m.get("graded_by") != null) {
                Long gradedBy = ((Number) m.get("graded_by")).longValue();
                User grader = userMapper.selectById(gradedBy);
                vo.setGradedByName(grader != null ? grader.getNickname() : "");
            }
            return vo;
        }).collect(Collectors.toList());

        IPage<SubmissionVO> result = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        result.setRecords(vos);
        return result;
    }

    public SubmissionVO getMySubmission(Long userId, Long assignmentId) {
        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }

        getMemberOrThrow(assignment.getClassroomId(), userId);

        ClassroomSubmission submission = classroomSubmissionMapper.selectByAssignmentAndStudent(assignmentId, userId);
        if (submission == null) {
            return null;
        }

        User student = userMapper.selectById(submission.getStudentId());
        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setAssignmentId(submission.getAssignmentId());
        vo.setStudentId(submission.getStudentId());
        vo.setStudentNickname(student != null ? student.getNickname() : "");
        vo.setStudentAvatar(student != null ? student.getAvatar() : "");
        vo.setReadingDuration(submission.getReadingDuration());
        vo.setAnnotationSummary(submission.getAnnotationSummary());
        vo.setPageProgress(submission.getPageProgress());
        vo.setProofImages(submission.getProofImages());
        vo.setSubmitAt(submission.getSubmitAt());
        vo.setStatus(submission.getStatus());
        vo.setScore(submission.getScore());
        vo.setTeacherComment(submission.getTeacherComment());
        vo.setGradedAt(submission.getGradedAt());
        if (submission.getGradedBy() != null) {
            User grader = userMapper.selectById(submission.getGradedBy());
            vo.setGradedByName(grader != null ? grader.getNickname() : "");
        }
        return vo;
    }

    @Transactional
    public ClassroomSubmission gradeSubmission(Long userId, Long submissionId, GradeDTO dto) {
        ClassroomSubmission submission = classroomSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }

        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(submission.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }

        ClassroomMember member = getMemberOrThrow(assignment.getClassroomId(), userId);
        validateTeacherRole(member);

        if (dto.getScore() < 0 || dto.getScore() > assignment.getTotalScore()) {
            throw new BusinessException("评分应在0到" + assignment.getTotalScore() + "之间");
        }

        boolean wasNotGraded = submission.getStatus() != 2;
        submission.setScore(dto.getScore());
        submission.setTeacherComment(dto.getTeacherComment() != null ? dto.getTeacherComment() : "");
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(userId);
        submission.setStatus(2);
        classroomSubmissionMapper.updateById(submission);

        if (wasNotGraded) {
            assignment.setGradedCount(assignment.getGradedCount() + 1);
            classroomAssignmentMapper.updateById(assignment);
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("bookTitle", assignment.getBookTitle());
        vars.put("score", String.valueOf(dto.getScore()));
        notificationService.sendByTemplate("classroom_graded", submission.getStudentId(), vars,
                "{\"type\":\"classroom_graded\",\"assignmentId\":" + assignment.getId() + "}");

        log.info("批改作业: teacherId={}, submissionId={}, score={}", userId, submissionId, dto.getScore());
        return submission;
    }

    @Transactional
    public void sendReminder(Long userId, Long assignmentId, ReminderDTO dto) {
        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }

        ClassroomMember member = getMemberOrThrow(assignment.getClassroomId(), userId);
        validateTeacherRole(member);

        ClassroomMember studentMember = classroomMemberMapper.selectByClassroomAndUser(
                assignment.getClassroomId(), dto.getStudentId());
        if (studentMember == null || studentMember.getRole() != 2) {
            throw new BusinessException("该学生不在班级中");
        }

        ClassroomSubmission existing = classroomSubmissionMapper.selectByAssignmentAndStudent(assignmentId, dto.getStudentId());
        if (existing != null && existing.getStatus() >= 1) {
            throw new BusinessException("该学生已提交作业");
        }

        ClassroomReminder reminder = new ClassroomReminder();
        reminder.setAssignmentId(assignmentId);
        reminder.setTeacherId(userId);
        reminder.setStudentId(dto.getStudentId());
        reminder.setMessage(dto.getMessage() != null ? dto.getMessage() : "");
        classroomReminderMapper.insert(reminder);

        Classroom classroom = classroomMapper.selectById(assignment.getClassroomId());
        Map<String, String> vars = new HashMap<>();
        vars.put("bookTitle", assignment.getBookTitle());
        vars.put("className", classroom != null ? classroom.getName() : "");
        notificationService.sendByTemplate("classroom_reminder", dto.getStudentId(), vars,
                "{\"type\":\"classroom_reminder\",\"assignmentId\":" + assignmentId + "}");

        log.info("催交作业: teacherId={}, assignmentId={}, studentId={}", userId, assignmentId, dto.getStudentId());
    }

    @Transactional
    public void batchRemind(Long userId, Long assignmentId) {
        ClassroomAssignment assignment = classroomAssignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }

        ClassroomMember member = getMemberOrThrow(assignment.getClassroomId(), userId);
        validateTeacherRole(member);

        List<Map<String, Object>> members = classroomMemberMapper.selectMembersWithUserInfo(assignment.getClassroomId());
        int remindCount = 0;
        for (Map<String, Object> m : members) {
            if (((Number) m.get("role")).intValue() != 2) continue;

            Long studentId = ((Number) m.get("user_id")).longValue();
            ClassroomSubmission existing = classroomSubmissionMapper.selectByAssignmentAndStudent(assignmentId, studentId);
            if (existing != null && existing.getStatus() >= 1) continue;

            ClassroomReminder lastReminder = classroomReminderMapper.selectLatestByAssignmentAndStudent(assignmentId, studentId);
            if (lastReminder != null && lastReminder.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1))) {
                continue;
            }

            ClassroomReminder reminder = new ClassroomReminder();
            reminder.setAssignmentId(assignmentId);
            reminder.setTeacherId(userId);
            reminder.setStudentId(studentId);
            reminder.setMessage("");
            classroomReminderMapper.insert(reminder);

            Classroom classroom = classroomMapper.selectById(assignment.getClassroomId());
            Map<String, String> vars = new HashMap<>();
            vars.put("bookTitle", assignment.getBookTitle());
            vars.put("className", classroom != null ? classroom.getName() : "");
            notificationService.sendByTemplate("classroom_reminder", studentId, vars,
                    "{\"type\":\"classroom_reminder\",\"assignmentId\":" + assignmentId + "}");

            remindCount++;
        }

        log.info("批量催交: teacherId={}, assignmentId={}, remindCount={}", userId, assignmentId, remindCount);
    }

    public ClassroomStatsVO getClassroomStats(Long userId, Long classroomId) {
        ClassroomMember member = getMemberOrThrow(classroomId, userId);
        validateTeacherRole(member);

        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException("班级不存在");
        }

        long totalStudents = classroomMemberMapper.selectCount(new LambdaQueryWrapper<ClassroomMember>()
                .eq(ClassroomMember::getClassroomId, classroomId)
                .eq(ClassroomMember::getRole, 2));

        List<ClassroomAssignment> assignments = classroomAssignmentMapper.selectByClassroomId(classroomId);
        long activeCount = assignments.stream().filter(a -> a.getStatus() == 1).count();

        ClassroomStatsVO stats = new ClassroomStatsVO();
        stats.setClassroomId(classroomId);
        stats.setClassroomName(classroom.getName());
        stats.setTotalStudents((int) totalStudents);
        stats.setTotalAssignments(assignments.size());
        stats.setActiveAssignments((int) activeCount);

        List<ClassroomStatsVO.AssignmentStatsItem> items = new ArrayList<>();
        double totalAvgScore = 0;
        double totalSubmitRate = 0;
        int scoredCount = 0;

        for (ClassroomAssignment a : assignments) {
            ClassroomStatsVO.AssignmentStatsItem item = new ClassroomStatsVO.AssignmentStatsItem();
            item.setAssignmentId(a.getId());
            item.setBookTitle(a.getBookTitle());
            item.setSubmitCount(a.getSubmitCount());
            item.setTotalMembers((int) totalStudents);
            item.setSubmitRate(totalStudents > 0 ? (double) a.getSubmitCount() / totalStudents : 0);
            item.setAvgScore(classroomSubmissionMapper.selectAvgScoreByAssignment(a.getId()));
            item.setGradedCount(a.getGradedCount());
            items.add(item);

            if (item.getAvgScore() > 0) {
                totalAvgScore += item.getAvgScore();
                scoredCount++;
            }
            totalSubmitRate += item.getSubmitRate();
        }

        stats.setOverallAvgScore(scoredCount > 0 ? totalAvgScore / scoredCount : 0);
        stats.setOverallSubmitRate(assignments.size() > 0 ? totalSubmitRate / assignments.size() : 0);
        stats.setAssignmentStats(items);

        return stats;
    }

    public IPage<Classroom> adminClassroomList(int page, int size, String keyword, Integer status) {
        Page<Classroom> pageParam = new Page<>(page, size);
        return classroomMapper.selectAdminPage(pageParam, keyword, status);
    }

    public Map<String, Object> adminDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalClassrooms", classroomMapper.countTotal());
        result.put("closedClassrooms", classroomMapper.countClosed());
        return result;
    }

    @Transactional
    public void closeClassroom(Long adminId, Long classroomId, String reason) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException("班级不存在");
        }
        classroom.setStatus(0);
        classroom.setBanReason(reason);
        classroom.setBannedAt(LocalDateTime.now());
        classroom.setBannedBy(adminId);
        classroomMapper.updateById(classroom);
        log.info("关闭班级: adminId={}, classroomId={}, reason={}", adminId, classroomId, reason);
    }

    @Transactional
    public void reopenClassroom(Long adminId, Long classroomId) {
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException("班级不存在");
        }
        classroom.setStatus(1);
        classroom.setBanReason("");
        classroom.setBannedAt(null);
        classroom.setBannedBy(null);
        classroomMapper.updateById(classroom);
        log.info("重新开放班级: adminId={}, classroomId={}", adminId, classroomId);
    }

    @Transactional
    public void closeExpiredAssignments() {
        List<ClassroomAssignment> expired = classroomAssignmentMapper.selectList(
                new LambdaQueryWrapper<ClassroomAssignment>()
                        .eq(ClassroomAssignment::getStatus, 1)
                        .le(ClassroomAssignment::getDeadline, LocalDateTime.now()));
        for (ClassroomAssignment a : expired) {
            a.setStatus(2);
            classroomAssignmentMapper.updateById(a);
            log.info("作业自动截止: assignmentId={}", a.getId());
        }
    }

    @Transactional
    public void setTeacherRole(Long userId, boolean isTeacher) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setIsTeacher(isTeacher ? 1 : 0);
        if (isTeacher) {
            user.setTeacherVerified(1);
        }
        userMapper.updateById(user);
        log.info("设置教师角色: userId={}, isTeacher={}", userId, isTeacher);
    }
}
