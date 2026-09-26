package com.dacs.bff.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsViewDto {
    private String title;
    private String contents;
    private String url;
    private Long date;
    private String dateFormatted;
    // placeholder for header image preview
    private String headerImageUrl;
}
