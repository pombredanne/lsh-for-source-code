package yapgen.base.knowledge.plan.experience;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import yapgen.base.Entity;
import yapgen.base.Word;
import yapgen.base.knowledge.character.CharacterCoordinate;
import yapgen.base.knowledge.character.need.NeedClass;
import yapgen.base.knowledge.fact.Fact;
import yapgen.base.knowledge.plan.decision.PlanSynthesis;
import yapgen.base.relation.RelationClass;
import yapgen.base.util.SerUtils;
import yapgen.base.engine.Engine;
import yapgen.base.engine.EngineConfigProperties;

/**
 *
 * @author riccardo
 */
public class FileMapBackend implements PlanExperienceBackend {

    private String folder;

    private String descriptor;

    public FileMapBackend() throws IOException {
        String baseFolder = EngineConfigProperties.ExperienceFolder;
        this.folder = baseFolder + "/" + getKnowledgeSpecificFolder();
        init();
    }

    @Override
    public void doPut(CharacterCoordinate coord, PlanSynthesis planSynthesis) throws IOException {
        File file = getCharacterCoordinateFile(coord);
        SerUtils.writeObjectToSerFile(planSynthesis, file.getParent(), file.getName());
    }

    @Override
    public PlanSynthesis doGet(CharacterCoordinate coord) throws IOException, ClassNotFoundException {
        File file = getCharacterCoordinateFile(coord);
        PlanSynthesis ret;
        try {
            ret = (PlanSynthesis) SerUtils.readObjectFromSerFile(file.getParent(), file.getName());
        } catch (FileNotFoundException e) {
            ret = null;
        }
        return ret;
    }

    private String getKnowledgeSpecificFolder() {
        StringBuilder descriptorBuilder = new StringBuilder();
        descriptorBuilder.append("Knowledge base:").append("\n").append("\n");
        for (Entry<Word, Entity> wordEntityEntry : Engine.getInstance().getEntityManager().getWordEntityMap().entrySet()) {
            Word word = wordEntityEntry.getKey();
            Entity entity = wordEntityEntry.getValue();
            if (entity instanceof Fact || entity instanceof NeedClass || entity instanceof RelationClass) {
                descriptorBuilder.append(word.getText()).append(" : ").append(entity.getClass().getSimpleName()).append("\n");
            }
        }
        this.descriptor = descriptorBuilder.toString();
        return md5Encode(this.descriptor);
    }

    private File getCharacterCoordinateFile(CharacterCoordinate coord) {
        String cellHashString = String.format("%016X", coord.getCellHash());
        StringBuilder relationsBuilder = new StringBuilder();
        for (Word w : coord.getRelationsWords()) {
            relationsBuilder.append("_").append(w.getText());
        }
        String fileName = String.format("%s_%s%s.ser", coord.getWord().getText(), cellHashString, relationsBuilder.toString());
        String fileFolder = folder + "/" + coord.getWord().getText();
        for (int i = 0; i < 7; i++) {
            fileFolder += "/" + cellHashString.substring(2 * i, 2 * i + 2);
        }
        return new File(fileFolder, fileName);
    }

    private void init() throws IOException {
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
            File f = new File(folder, "descriptor.txt");
            FileWriter fw = new FileWriter(f);
            fw.write(descriptor);
            fw.close();
        }
    }

    public String md5Encode(String str) {
        byte data[] = str.getBytes();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(FileMapBackend.class.getName()).log(Level.SEVERE, null, ex);
        }
        digest.update(data);
        byte[] hash = digest.digest();
        String hex = "";
        StringBuilder hashString = new StringBuilder();
        for (int i = 0; i < hash.length; ++i) {
            hex = Integer.toHexString((int) hash[i]);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            hashString.append(hex.substring(hex.length() - 2));
        }
        return hashString.toString();
    }
}
