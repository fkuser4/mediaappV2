package com.tvz.mediaapp.dto;

import lombok.Data;
import java.util.List;

@Data
public class DownloadRequestDto {
    private String postUuid;
    private List<String> filenames;
}
