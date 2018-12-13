package com.fbx;

import java.io.Serializable;

public class CameraData implements Serializable {

    // 摄像头ID
    private int cameraId;
    // 设备序列号（用于播放）
    private String cameraSns;
    // 设备描述，标题
    private String cameraTitle;
    // 设备快照资源地址
    private String snapshotUrl;
    // 设备状态
    private Boolean online;
    // 设备号
    private int cameraNo;

    public CameraData(String cameraSns) {
        this.cameraSns = cameraSns;
        this.online = true;
    }

    public CameraData(int cameraId, String cameraSns, int cameraNo, String cameraTitle, String snapshotUrl, Boolean isOnline) {
        this.cameraSns = cameraSns;
        this.cameraTitle = cameraTitle;
        this.snapshotUrl = snapshotUrl;
        this.online = isOnline;
        this.cameraNo = cameraNo;
        this.cameraId = cameraId;
    }

    public String getCameraSns() {
        return cameraSns;
    }

    public void setCameraSns(String cameraSns) {
        this.cameraSns = cameraSns;
    }

    public String getCameraTitle() {
        return cameraTitle;
    }

    public void setCameraTitle(String cameraTitle) {
        this.cameraTitle = cameraTitle;
    }

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }

    public Boolean getOnline() {
        return online;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public void setCameraNo(int cameraNo) {
        this.cameraNo = cameraNo;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public int getCameraNo() {
        return cameraNo;
    }

    public void getCameraNo(int cameraNo) {
        this.cameraNo = cameraNo;
    }


}
