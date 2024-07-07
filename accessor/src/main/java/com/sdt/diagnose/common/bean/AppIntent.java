package com.sdt.diagnose.common.bean;

import java.util.List;

import androidx.annotation.NonNull;

public class AppIntent {
    private List<AppBean> beanList;

    public List<AppBean> getBeanList() {
        return beanList;
    }

    public void setBeanList(List<AppBean> beanList) {
        this.beanList = beanList;
    }

    @Override
    @NonNull
    public String toString() {
        return "AppIntent{" + "beanList=" + beanList + '}';
    }

    public static class AppBean {
        private String fileName;
        private long fileSize;
        private String fileMd5;
        private String fileVersion;
        private String fileUrl;
        private int upgradeMode;
        private int upgradeType;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileMd5() {
            return fileMd5;
        }

        public void setFileMd5(String fileMd5) {
            this.fileMd5 = fileMd5;
        }

        public String getFileVersion() {
            return fileVersion;
        }

        public void setFileVersion(String fileVersion) {
            this.fileVersion = fileVersion;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public int getUpgradeMode() {
            return upgradeMode;
        }

        public void setUpgradeMode(int upgradeMode) {
            this.upgradeMode = upgradeMode;
        }

        public int getUpgradeType() {
            return upgradeType;
        }

        public void setUpgradeType(int upgradeType) {
            this.upgradeType = upgradeType;
        }

        @Override
        @NonNull
        public String toString() {
            return "AppBean{"
                    + "fileName='"
                    + fileName
                    + '\''
                    + ", fileSize="
                    + fileSize
                    + ", fileMd5='"
                    + fileMd5
                    + '\''
                    + ", fileVersion='"
                    + fileVersion
                    + '\''
                    + ", fileUrl='"
                    + fileUrl
                    + '\''
                    + ", upgradeMode="
                    + upgradeMode
                    + ", upgradeType="
                    + upgradeType
                    + '}';
        }
    }
}
