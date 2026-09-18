package com.construction.site.service;

import com.construction.site.dto.BizException;
import com.construction.site.entity.Yard;
import com.construction.site.repository.YardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class YardService {

    private final YardRepository yards;

    public YardService(YardRepository yards) {
        this.yards = yards;
    }

    /** 列表：可以按状态筛，也可以按编号/名称模糊搜。 */
    public List<Yard> query(String state, String keyword) {
        return yards.findAllByOrderByIdAsc().stream()
                .filter(y -> state == null || state.isBlank() || state.equals(y.state))
                .filter(y -> hit(y, keyword))
                .toList();
    }

    private boolean hit(Yard y, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String k = keyword.trim();
        return y.no.contains(k) || y.title.contains(k);
    }

    @Transactional
    public Yard register(Yard form) {
        if (form.no == null || form.no.isBlank()) {
            throw new BizException("堆场编号得填");
        }
        if (form.title == null || form.title.isBlank()) {
            throw new BizException("堆场名称得填");
        }
        if (yards.findByNo(form.no.trim()).isPresent()) {
            throw new BizException("编号 " + form.no.trim() + " 已经用过了");
        }
        if (form.areaSize != null && form.areaSize <= 0) {
            throw new BizException("占地面积要大于 0");
        }
        if (form.maxLoad != null && form.maxLoad <= 0) {
            throw new BizException("可堆量要大于 0");
        }
        form.no = form.no.trim();
        form.state = (form.state == null || form.state.isBlank()) ? "可用" : form.state;
        return yards.save(form);
    }

    @Transactional
    public Yard modify(Long id, Yard form) {
        Yard y = yards.findById(id).orElseThrow(() -> new BizException("这个堆场找不到了"));

        if (form.no != null && !form.no.isBlank() && !form.no.trim().equals(y.no)) {
            if (yards.findByNo(form.no.trim()).isPresent()) {
                throw new BizException("编号 " + form.no.trim() + " 已经用过了");
            }
            y.no = form.no.trim();
        }
        if (form.title != null && !form.title.isBlank()) {
            y.title = form.title;
        }
        if (form.areaSize != null) {
            if (form.areaSize <= 0) {
                throw new BizException("占地面积要大于 0");
            }
            y.areaSize = form.areaSize;
        }
        if (form.maxLoad != null) {
            if (form.maxLoad <= 0) {
                throw new BizException("可堆量要大于 0");
            }
            y.maxLoad = form.maxLoad;
        }
        if (form.state != null && !form.state.isBlank() && !form.state.equals(y.state)) {
            if ("停用".equals(form.state) && yards.countMaterialsIn(id) > 0) {
                throw new BizException("这个堆场里还堆着材料，先把材料清空再停用");
            }
            y.state = form.state;
        }
        return yards.save(y);
    }
}
