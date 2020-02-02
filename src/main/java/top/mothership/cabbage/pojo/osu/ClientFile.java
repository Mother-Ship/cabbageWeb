package top.mothership.cabbage.pojo.osu;

import lombok.Data;

@Data
public class ClientFile {
    private String fileVersion;
    private String fileName;
    private String fileHash;
    private String filesize;
    private String timestamp;
    private String patch_id;
    private String urlFull;
}
