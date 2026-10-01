package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController @RequestMapping("/api/admin/fortune-operations") @RequiredArgsConstructor
public class FortuneOperationsAdminController {
  private final FortuneOperationsAdminService service;
  @GetMapping("/summary") public ApiResponse<?> summary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
    if (!from.isBefore(to) || from.plusDays(93).isBefore(to)) throw new IllegalArgumentException("기간은 1~93일이어야 합니다.");
    return ApiResponse.ok(Map.of("kpi",service.summary(from,to),"funnel",service.funnel(from,to)));
  }
}
