package com.construction.site.service;

import com.construction.site.dto.BizException;
import com.construction.site.entity.SafetyInspection;
import com.construction.site.entity.Yard;
import com.construction.site.repository.SafetyInspectionRepository;
import com.construction.site.repository.YardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 安全巡检：打分、判定、整改闭环。 */
@Service
public class SafetyInspectionService {

    private final SafetyInspectionRepository inspections;
    private final YardRepository yards;

    public SafetyInspectionService(SafetyInspectionRepository inspections, YardRepository yards) {
        this.inspections = inspections;
        this.yards = yards;
    }

    public List<SafetyInspection> query(Long yardId, String state, String keyword) {
        return inspections.findAllByOrderByIdAsc().stream()
                .filter(i -> yardId == null || yardId.equals(i.yardId))
                .filter(i -> state == null || state.isBlank() || state.equals(i.state))
                .filter(i -> keyword == null || keyword.isBlank()
                        || i.no.contains(keyword.trim()) || i.inspector.contains(keyword.trim()))
                .toList();
    }

    @Transactional
    public SafetyInspection register(SafetyInspection form) {
        if (form.no == null || form.no.isBlank()) {
            throw new BizException("巡检单号得填");
        }
        if (inspections.findByNo(form.no.trim()).isPresent()) {
            throw new BizException("巡检单号 " + form.no.trim() + " 已经用过了");
        }
        if (form.yardId == null) {
            throw new BizException("巡检得指明是哪个堆场");
        }
        Yard yard = yards.findById(form.yardId).orElseThrow(() -> new BizException("被巡检的堆场不存在"));
        checkScoreAndVerdict(form.score, form.verdict);

        form.no = form.no.trim();
        form.inspector = form.inspector == null || form.inspector.isBlank() ? "未署名" : form.inspector;
        form.yardId = yard.id;
        form.state = "合格".equals(form.verdict) ? "已闭环" : "待整改";
        return inspections.save(form);
    }

    @Transactional
    public SafetyInspection modify(Long id, SafetyInspection form) {
        SafetyInspection i = inspections.findById(id).orElseThrow(() -> new BizException("这条巡检记录找不到了"));

        Integer score = form.score == null ? i.score : form.score;
        String verdict = form.verdict == null || form.verdict.isBlank() ? i.verdict : form.verdict;
        checkScoreAndVerdict(score, verdict);

        if (form.score != null) {
            i.score = form.score;
        }
        if (form.verdict != null && !form.verdict.isBlank()) {
            i.verdict = form.verdict;
            // 判定一改，整改状态要跟着走：改判合格就直接闭环
            if ("合格".equals(form.verdict)) {
                i.state = "已闭环";
            } else if (!"待整改".equals(i.state)) {
                i.state = "待整改";
            }
        }
        if (form.inspector != null && !form.inspector.isBlank()) {
            i.inspector = form.inspector;
        }
        if (form.inspectDate != null) {
            i.inspectDate = form.inspectDate;
        }
        if (form.state != null && !form.state.isBlank() && !form.state.equals(i.state)) {
            if ("已闭环".equals(form.state) && "不合格".equals(i.verdict) && i.score != null && i.score < 60) {
                throw new BizException("这次巡检不合格（" + i.score + " 分），分数没改上来之前不能直接闭环");
            }
            i.state = form.state;
        }
        return inspections.save(i);
    }

    /** 分数与判定必须自洽：不合格的得分不该及格，及格的得分不该低于 60。 */
    private void checkScoreAndVerdict(Integer score, String verdict) {
        if (score != null && (score < 0 || score > 100)) {
            throw new BizException("巡检得分要落在 0 到 100 之间");
        }
        if (score == null || verdict == null || verdict.isBlank()) {
            return;
        }
        if ("不合格".equals(verdict) && score >= 60) {
            throw new BizException("判成不合格，可得分有 " + score + "，这不一致");
        }
        if ("合格".equals(verdict) && score < 60) {
            throw new BizException("只得了 " + score + " 分，判合格说不过去");
        }
    }
}
