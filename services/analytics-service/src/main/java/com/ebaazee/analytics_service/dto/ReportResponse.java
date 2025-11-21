package com.ebaazee.analytics_service.dto;

public class ReportResponse {
    private String filename;
    private String base64Content;

    public ReportResponse() {}

    public ReportResponse(String filename, String base64Content) {
        this.filename = filename;
        this.base64Content = base64Content;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }
}
