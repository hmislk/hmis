/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.bedboard;

import java.io.Serializable;

/**
 * Lightweight DTO for the bed-board SVG sub-resources (issue #21592).
 *
 * Used by GET/PUT {@code /api/institutions/{id}/svg},
 * {@code /api/sites/{id}/svg}, and {@code /api/departments/{id}/svg} so an
 * agent can read and set the two SVG drawings without round-tripping the whole
 * entity.
 *
 * <p>Each bed-board entity stores two drawings on a shared
 * {@code viewBox="0 0 1000 600"} grid:
 * <ul>
 *   <li>{@code svgParentView} — the entity's own empty floor-plan canvas,
 *       shown when you navigate <em>into</em> it.</li>
 *   <li>{@code svgChildView} — the small shape showing how this entity looks as
 *       a tile <em>inside its parent's</em> canvas.</li>
 * </ul>
 *
 * <p>On a PUT, only fields present (non-null) are changed; pass an empty string
 * to clear a drawing. SVG markup is stored verbatim (create/update → read back
 * identical); the bed board sanitises it at render time.
 *
 * @author Buddhika
 */
public class BedBoardSvgDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String svgParentView;
    private String svgChildView;

    public BedBoardSvgDTO() {
    }

    public BedBoardSvgDTO(Long id, String name, String svgParentView, String svgChildView) {
        this.id = id;
        this.name = name;
        this.svgParentView = svgParentView;
        this.svgChildView = svgChildView;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSvgParentView() {
        return svgParentView;
    }

    public void setSvgParentView(String svgParentView) {
        this.svgParentView = svgParentView;
    }

    public String getSvgChildView() {
        return svgChildView;
    }

    public void setSvgChildView(String svgChildView) {
        this.svgChildView = svgChildView;
    }
}
