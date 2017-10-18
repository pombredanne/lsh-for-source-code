package com.nullfish.lib.vfs.manipulation.common;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import com.nullfish.lib.vfs.VFile;
import com.nullfish.lib.vfs.exception.ManipulationStoppedException;
import com.nullfish.lib.vfs.exception.VFSException;
import com.nullfish.lib.vfs.exception.VFSIOException;
import com.nullfish.lib.vfs.exception.VFSSystemException;
import com.nullfish.lib.vfs.manipulation.abst.AbstractManipulation;

/**
 * @author shunji
 *
 */
public class MD5HashManipulation extends AbstractManipulation {

    /**
	 * ハッシュ値
	 */
    private byte[] hash;

    /**
	 * メッセージのパラメータ
	 */
    protected VFile[] messageParam;

    /**
	 * 経過フォーマット
	 */
    private MessageFormat progressFormat;

    /**
	 * コンストラクタ
	 * @param file	操作対象ファイル
	 */
    public MD5HashManipulation(VFile file) {
        super(file);
        messageParam = new VFile[1];
        messageParam[0] = file;
    }

    public void doExecute() throws VFSException {
        BufferedInputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new BufferedInputStream(new DigestInputStream(file.getInputStream(), digest));
            byte[] buffer = new byte[4096];
            while (is.read(buffer) != -1) {
                if (isStopped()) {
                    throw new ManipulationStoppedException(this);
                }
            }
            hash = digest.digest();
        } catch (IOException e) {
            throw new VFSIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new VFSSystemException(e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * ハッシュ値を取得する。
	 * @return	ハッシュ値
	 */
    public byte[] getHash() {
        return hash;
    }

    /**
	 * 作業経過メッセージを取得する。
	 * 
	 * @return
	 */
    public String getProgressMessage() {
        if (progressFormat == null) {
            progressFormat = new MessageFormat(progressMessages.getString("md5_hash"));
        }
        return progressFormat.format(messageParam);
    }
}
