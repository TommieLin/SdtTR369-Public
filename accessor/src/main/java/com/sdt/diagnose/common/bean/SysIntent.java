package com.sdt.diagnose.common.bean;

import java.util.List;

import androidx.annotation.NonNull;

public class SysIntent {
    private List<SysBean> beanList;

    public List<SysBean> getBeanList() {
        return beanList;
    }

    public void setBeanList(List<SysBean> beanList) {
        this.beanList = beanList;
    }

    @Override
    @NonNull
    public String toString() {
        return "SysIntent{" + "beanList=" + beanList + '}';
    }

    public static class SysBean {
        private String fileName;
        private long fileSize;
        private String fileMd5;
        private String fileVersion;
        private String fileUrl;
        private int upgradeMode;
        private int upgradeType;
        private String abFileHash;
        private String abFileSize;
        private String abMetadataHash;
        private String abMetadataSize;

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

        public String getAbFileHash() {
            return abFileHash;
        }

        public void setAbFileHash(String abFileHash) {
            this.abFileHash = abFileHash;
        }

        public String getAbFileSize() {
            return abFileSize;
        }

        public void setAbFileSize(String abFileSize) {
            this.abFileSize = abFileSize;
        }

        public String getAbMetadataHash() {
            return abMetadataHash;
        }

        public void setAbMetadataHash(String abMetadataHash) {
            this.abMetadataHash = abMetadataHash;
        }

        public String getAbMetadataSize() {
            return abMetadataSize;
        }

        public void setAbMetadataSize(String abMetadataSize) {
            this.abMetadataSize = abMetadataSize;
        }

        @Override
        @NonNull
        public String toString() {
            return "SysBean{"
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
                    + ", abFileHash='"
                    + abFileHash
                    + '\''
                    + ", abFileSize='"
                    + abFileSize
                    + '\''
                    + ", abMetadataHash='"
                    + abMetadataHash
                    + '\''
                    + ", abMetadataSize='"
                    + abMetadataSize
                    + '\''
                    + '}';
        }
    }
}
