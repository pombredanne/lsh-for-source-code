package org.grailrtls.gui.network;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

public class RegionInfo {

    public final String name;

    public final int databaseID;

    public final int units;

    public final float xMin;

    public final float xMax;

    public final float yMin;

    public final float yMax;

    public final float zMin;

    public final float zMax;

    private final int hashcode;

    public final List<TrainingInfo> trainingInfo = Collections.synchronizedList(new ArrayList<TrainingInfo>());

    public final BufferedImage mapImage;

    public RegionInfo(String name, int databaseID, int units, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax, String imageURL) {
        this.name = name;
        this.databaseID = databaseID;
        this.units = units;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(this.name.getBytes());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.writeInt(this.databaseID);
            daos.writeInt(this.units);
            daos.writeDouble(this.xMin);
            daos.writeDouble(this.xMax);
            daos.writeDouble(this.yMin);
            daos.writeDouble(this.yMax);
            daos.writeDouble(this.zMin);
            daos.writeDouble(this.zMax);
            daos.flush();
            byte[] hashValue = digest.digest(baos.toByteArray());
            int hashCode = 0;
            for (int i = 0; i < hashValue.length; i++) {
                hashCode += (int) hashValue[i] << (i % 4);
            }
            this.hashcode = hashCode;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error occurred while generating hashcode for region " + this.name);
        }
        if (imageURL != null) {
            URL url = null;
            try {
                url = new URL(imageURL);
            } catch (MalformedURLException murle) {
            }
            if (url != null) {
                BufferedImage tmpImage = null;
                try {
                    tmpImage = ImageIO.read(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mapImage = tmpImage;
            } else this.mapImage = null;
        } else this.mapImage = null;
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RegionInfo) return this.hashcode == ((RegionInfo) o).hashcode;
        return super.equals(o);
    }

    public String toString() {
        return this.name;
    }
}
