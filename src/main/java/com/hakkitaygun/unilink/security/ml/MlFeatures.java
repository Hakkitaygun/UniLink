package com.hakkitaygun.unilink.security.ml;

/** ML servisine gönderilen 10 özellik (features.sql v1 ile aynı). */
public record MlFeatures(
        int reqCount, int distinctPaths, double err4xxRatio, int c401, int c403, int c404,
        int loginFailCount, int distinctLoginIds, double writeRatio, int adminPathHits
) {}