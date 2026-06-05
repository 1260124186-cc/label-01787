package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.dto.CopyrightComplaintDTO;
import com.xiaoan.bookstore.entity.CopyrightComplaint;
import com.xiaoan.bookstore.service.ContentComplianceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mp/compliance")
@RequiredArgsConstructor
public class MpComplianceController {

    private final ContentComplianceService contentComplianceService;

    @PostMapping("/complaint")
    @Log("提交版权申诉")
    public Result<CopyrightComplaint> submitComplaint(@Valid @RequestBody CopyrightComplaintDTO dto) {
        return Result.success(contentComplianceService.submitComplaint(dto));
    }
}
