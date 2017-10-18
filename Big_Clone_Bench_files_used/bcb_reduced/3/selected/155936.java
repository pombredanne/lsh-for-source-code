package l1j.server.server;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import l1j.server.Base64;
import l1j.server.Config;
import l1j.server.L1DatabaseFactory;
import l1j.server.server.clientpackets.C_Who;
import l1j.server.server.command.L1Commands;
import l1j.server.server.command.executor.L1CommandExecutor;
import l1j.server.server.datatables.IpTable;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.datatables.NpcSpawnTable;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.datatables.PolyTable;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.datatables.SpawnTable;
import l1j.server.server.model.L1DwarfInventory;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1NpcDeleteTimer;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Party;
import l1j.server.server.model.L1PetRace;
import l1j.server.server.model.L1PolyMorph;
import l1j.server.server.model.L1Spawn;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.Instance.L1TrapInstance;
import l1j.server.server.model.item.L1ItemId;
import l1j.server.server.model.skill.L1SkillId;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.model.trap.L1WorldTraps;
import l1j.server.server.model.L1Clan;
import l1j.server.server.model.event.BugRace;
import l1j.server.server.serverpackets.S_Chainfo;
import l1j.server.server.serverpackets.S_RemoveObject;
import l1j.server.server.serverpackets.S_Disconnect;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_HPMeter;
import l1j.server.server.serverpackets.S_Invis;
import l1j.server.server.serverpackets.S_Message_YN;
import l1j.server.server.serverpackets.S_OpCode_Test;
import l1j.server.server.serverpackets.S_OtherCharPacks;
import l1j.server.server.serverpackets.S_PacketBox;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.S_SkillBrave;
import l1j.server.server.serverpackets.S_SkillHaste;
import l1j.server.server.serverpackets.S_SkillIconGFX;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.S_Weather;
import l1j.server.server.serverpackets.S_WhoAmount;
import l1j.server.server.serverpackets.S_ChinSword;
import l1j.server.server.templates.L1Command;
import l1j.server.server.templates.L1Item;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1Skills;
import static l1j.server.server.model.skill.L1SkillId.*;

public class GMCommands {

    private static Logger _log = Logger.getLogger(GMCommands.class.getName());

    boolean spawnTF = false;

    private static GMCommands _instance;

    private GMCommands() {
    }

    public static GMCommands getInstance() {
        if (_instance == null) {
            _instance = new GMCommands();
        }
        return _instance;
    }

    private String complementClassName(String className) {
        if (className.contains(".")) {
            return className;
        }
        return "l1j.server.server.command.executor." + className;
    }

    private boolean executeDatabaseCommand(L1PcInstance pc, String name, String arg) {
        try {
            L1Command command = L1Commands.get(name);
            if (command == null) {
                return false;
            }
            if (pc.getAccessLevel() < command.getLevel()) {
                pc.sendPackets(new S_ServerMessage(74, "��ɾ�" + name));
                return true;
            }
            Class<?> cls = Class.forName(complementClassName(command.getExecutorClassName()));
            L1CommandExecutor exe = (L1CommandExecutor) cls.getMethod("getInstance").invoke(null);
            exe.execute(pc, name, arg);
            l1j.server.Leaf.tarea.append("\r\n[�ý���] " + pc.getName() + "���� " + name + " " + arg + " ��ɾ ����߽��ϴ�.");
            _log.info(pc.getName() + "���� " + name + " " + arg + " ��ɾ ����߽��ϴ�.");
            return true;
        } catch (Exception e) {
            _log.log(Level.SEVERE, "error gm command", e);
        }
        return false;
    }

    public void handleCommands(L1PcInstance gm, String cmdLine) {
        StringTokenizer token = new StringTokenizer(cmdLine);
        String cmd = token.nextToken();
        String param = "";
        while (token.hasMoreTokens()) {
            param = new StringBuilder(param).append(token.nextToken()).append(' ').toString();
        }
        param = param.trim();
        if (executeDatabaseCommand(gm, cmd, param)) {
            return;
        }
        try {
            if (gm.getAccessLevel() < 100) {
                gm.sendPackets(new S_ServerMessage(74, "��ɾ�" + cmd));
                return;
            }
            if (gm.getAccessLevel() < 200) {
                gm.sendPackets(new S_ServerMessage(74, "��ɾ�" + cmd));
                return;
            }
            if (gm.getInventory().checkEquipped(300000)) {
                if (cmd.equalsIgnoreCase("����")) {
                    showHelp(gm);
                } else if (cmd.equalsIgnoreCase("��ü��ȯ")) {
                    allrecall(gm);
                } else if (cmd.equalsIgnoreCase("���ͼ�ȯ")) {
                    allrecall(gm);
                } else if (cmd.equalsIgnoreCase("�ӵ�")) {
                    speed(gm);
                } else if (cmd.equalsIgnoreCase("��ȯ")) {
                    recall(gm, param);
                } else if (cmd.equalsIgnoreCase("��Ƽ��ȯ")) {
                    partyrecall(gm, param);
                } else if (cmd.equalsIgnoreCase("�̵�")) {
                    teleportTo(gm, param);
                } else if (cmd.equalsIgnoreCase("�׾��")) {
                    kill(gm, param);
                } else if (cmd.equalsIgnoreCase("��Ȱ")) {
                    ress(gm);
                } else if (cmd.equalsIgnoreCase("�Ƶ���")) {
                    adena(gm, param);
                } else if (cmd.equalsIgnoreCase("���")) {
                    moveToChar(gm, param);
                } else if (cmd.equalsIgnoreCase("������")) {
                    tospawn(gm, param);
                } else if (cmd.equalsIgnoreCase("����")) {
                    invisible(gm);
                } else if (cmd.equalsIgnoreCase("������")) {
                    visible(gm);
                } else if (cmd.equalsIgnoreCase("����")) {
                    changeWeather(gm, param);
                } else if (cmd.equalsIgnoreCase("��ȯ")) {
                    gmRoom(gm, param);
                } else if (cmd.equalsIgnoreCase("�����߹�")) {
                    powerkick(gm, param);
                } else if (cmd.equalsIgnoreCase("�߹�")) {
                    kick(gm, param);
                } else if (cmd.equalsIgnoreCase("�����߹�")) {
                    accbankick(gm, param);
                } else if (cmd.equalsIgnoreCase("����")) {
                    burf(gm, param);
                } else if (cmd.equalsIgnoreCase("������")) {
                    buff(gm, param, true);
                } else if (cmd.equalsIgnoreCase("����")) {
                    buff(gm, param, false);
                } else if (cmd.equalsIgnoreCase("�ù���")) {
                    allBuff(gm);
                } else if (cmd.equalsIgnoreCase("����")) {
                    spawn(gm, param);
                } else if (cmd.equalsIgnoreCase("���Ǿ�")) {
                    npcSpawn(gm, param, "npc");
                } else if (cmd.equalsIgnoreCase("������")) {
                    npcSpawn(gm, param, "mob");
                } else if (cmd.equalsIgnoreCase("����")) {
                    polymorph(gm, param);
                } else if (cmd.equalsIgnoreCase("�ۼ�Ʈ")) {
                    makeItemSet(gm, param);
                } else if (cmd.equalsIgnoreCase("������")) {
                    givesItem(gm, param);
                } else if (cmd.equalsIgnoreCase("ä��")) {
                    chatng(gm, param);
                } else if (cmd.equalsIgnoreCase("ä��")) {
                    chat(gm, param);
                } else if (cmd.equalsIgnoreCase("����")) {
                    present(gm, param);
                } else if (cmd.equalsIgnoreCase("��������")) {
                    lvPresent(gm, param);
                } else if (cmd.equalsIgnoreCase("�ٷ�����")) {
                    shutdownNow();
                } else if (cmd.equalsIgnoreCase("�������")) {
                    shutdownAbort();
                } else if (cmd.equalsIgnoreCase("����")) {
                    shutdown(gm, param);
                } else if (cmd.equalsIgnoreCase("����Ʈ��")) {
                    resetTrap();
                } else if (cmd.equalsIgnoreCase("ȨŸ��")) {
                    hometown(gm, param);
                } else if (cmd.equalsIgnoreCase("����")) {
                    gfxId(gm, param);
                } else if (cmd.equalsIgnoreCase("�κ�")) {
                    invGfxId(gm, param);
                } else if (cmd.equalsIgnoreCase("�׼�")) {
                    action(gm, param);
                } else if (cmd.equalsIgnoreCase("�������")) {
                    banIp(gm, param);
                } else if (cmd.equalsIgnoreCase("����")) {
                    who(gm, param);
                } else if (cmd.equalsIgnoreCase("����")) {
                    patrol(gm);
                } else if (cmd.equalsIgnoreCase("skick")) {
                    skick(gm, param);
                } else if (cmd.equalsIgnoreCase("�ǹ�")) {
                    hpBar(gm, param);
                } else if (cmd.equalsIgnoreCase("��Ʈ��")) {
                    showTraps(gm, param);
                } else if (cmd.equalsIgnoreCase("reloadtrap")) {
                    reloadTraps();
                } else if (cmd.equalsIgnoreCase("r")) {
                    redo(gm, param);
                } else if (cmd.equalsIgnoreCase("f")) {
                    favorite(gm, param);
                } else if (cmd.equalsIgnoreCase("gm")) {
                    gm(gm);
                } else if (cmd.equalsIgnoreCase("��æ�˻�")) {
                    checkEnchant(gm, param);
                } else if (cmd.equalsIgnoreCase("�Ƶ��˻�")) {
                    checkAden(gm, param);
                } else if (cmd.equalsIgnoreCase("�˻�")) {
                    chainfo(gm, param);
                } else if (cmd.equalsIgnoreCase("�����߰�")) {
                    accountadd(gm, param);
                } else if (cmd.equalsIgnoreCase("��ȣ����")) {
                    changePassword(gm, param);
                } else if (cmd.equalsIgnoreCase("�극�̽�")) {
                    PetRace(gm);
                } else if (cmd.equalsIgnoreCase("�������")) {
                    BugRace();
                } else if (cmd.equalsIgnoreCase("�𽺺�")) {
                    disbi(gm);
                } else if (cmd.equalsIgnoreCase("����")) {
                    question(gm, param);
                } else if (cmd.equalsIgnoreCase("���")) {
                    result(gm, param);
                } else if (cmd.equalsIgnoreCase("�˻�")) {
                    searchDatabase(gm, param);
                } else if (cmd.equalsIgnoreCase("��Ƣ��")) {
                    setFakeAmount(gm, param);
                } else if (cmd.equalsIgnoreCase("���")) {
                    draplist(gm, param);
                } else if (cmd.equalsIgnoreCase("opcid2")) {
                    opcId2(gm, param);
                } else if (cmd.equalsIgnoreCase("opcid1")) {
                    opcId1(gm, param);
                } else if (cmd.equalsIgnoreCase("opcid")) {
                    opcId(gm, param);
                } else if (cmd.equalsIgnoreCase("opc2")) {
                    opc2(gm, param);
                } else if (cmd.equalsIgnoreCase("opc1")) {
                    opc1(gm, param);
                } else if (cmd.equalsIgnoreCase("opc")) {
                    opc(gm, param);
                }
            } else {
                gm.sendPackets(new S_SystemMessage("����� ��ڰ� �� ������ ���� �ʽ��ϴ�."));
                return;
            }
            if (!cmd.equalsIgnoreCase("r")) {
                _lastCmd = cmdLine;
            }
        } catch (Exception e) {
            _log.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void showHelp(L1PcInstance pc) {
        pc.sendPackets(new S_SystemMessage("-------------------<GM ��ɾ�>---------------------"));
        pc.sendPackets(new S_SystemMessage(".���� .���� .û�� .��ü��ȯ .��ų�߰� .�ӵ� .����"));
        pc.sendPackets(new S_SystemMessage(".��ȯ .��� .��Ƽ��ȯ .�̵� .��ġ .�׾�� .��Ȱ  "));
        pc.sendPackets(new S_SystemMessage(".�Ƶ��� .���� .������ .���� .��ȯ .���� .�ù���"));
        pc.sendPackets(new S_SystemMessage(".�߹� .�����߹� .�����߹� .���� .���Ǿ� .������"));
        pc.sendPackets(new S_SystemMessage(".���� .������ .ä�� .ä�� .���� .���� .��������"));
        pc.sendPackets(new S_SystemMessage(".ȨŸ�� .���� .�ٷ����� .������� .������� .����"));
        pc.sendPackets(new S_SystemMessage(".�˻� .���� .�Ƶ��˻� .��æ�˻� .�����߰�.�𽺺�"));
        pc.sendPackets(new S_SystemMessage(".�ǹ� .������� .�극�̽� .��ȣ���� .skick"));
        pc.sendPackets(new S_SystemMessage(".���� .��� .���ͼ�ȯ .��Ƣ�� .�˻� .���"));
        pc.sendPackets(new S_SystemMessage("---------------------------------------------------"));
    }

    private void BugRace() {
        BugRace.getInstance();
    }

    private void setFakeAmount(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            int amount = Integer.parseInt(st.nextToken());
            C_Who.FAKE_AMOUNT = amount;
            gm.sendPackets(new S_SystemMessage("���� ��Ƣ��� [" + amount + "]�� �Դϴ�."));
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".��Ƣ�� [��Ƣ���] ������� �Է��� �ּ���. ���绽Ƣ��� [" + C_Who.FAKE_AMOUNT + "]��"));
        }
    }

    private void polyEvent(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            String isStart = st.nextToken();
            if ("����".equals(isStart)) {
                PolyTable.IS_POLY_EVENT = true;
                L1World.getInstance().broadcastServerMessage("�����̺�Ʈ�� ���۵Ǿ���ϴ�.");
            } else if ("����".equals(isStart)) {
                PolyTable.IS_POLY_EVENT = false;
                L1World.getInstance().broadcastServerMessage("�����̺�Ʈ�� ����Ǿ���ϴ�.");
            }
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".�����̺�Ʈ [����/����] ������� �Է��� �ּ���."));
        }
    }

    private void draplist(L1PcInstance gm, String param) {
        try {
            StringTokenizer tok = new StringTokenizer(param);
            String name = tok.nextToken().trim();
            drap_list(gm, name);
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".��� [�����۹�ȣ]"));
        }
    }

    private void drap_list(L1PcInstance gm, String name) {
        try {
            String s_mobid = null;
            int count = 0;
            java.sql.Connection con = null;
            con = L1DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = null;
            String strQry = "	Select b.name  " + " from droplist A " + " left outer join npc B on a.mobId = B.npcid " + "	where a.itemid ='" + name + "'" + "	group by b.name ";
            statement = con.prepareStatement(strQry);
            ResultSet rs = statement.executeQuery();
            String Data = "";
            while (rs.next()) {
                s_mobid = rs.getString(1);
                Data = Data + "[" + s_mobid + "]";
                count++;
                if (count % 1 == 0) {
                    gm.sendPackets(new S_SystemMessage("����: " + Data));
                    Data = "";
                }
            }
            rs.close();
            statement.close();
            con.close();
            gm.sendPackets(new S_SystemMessage("��������� �� [" + count + "]���� �����Ͱ� �˻��Ǿ���ϴ�."));
        } catch (Exception e) {
        }
    }

    private void speed(L1PcInstance pc) {
        int objectId = pc.getId();
        try {
            int time = 3600 * 1000;
            pc.setSkillEffect(L1SkillId.STATUS_BRAVE, time);
            pc.sendPackets(new S_SkillBrave(objectId, 1, 3600));
            pc.sendPackets(new S_SkillSound(objectId, 751));
            pc.broadcastPacket(new S_SkillSound(objectId, 751));
            pc.setBraveSpeed(1);
            pc.setSkillEffect(L1SkillId.STATUS_HASTE, time);
            pc.sendPackets(new S_SkillHaste(objectId, 1, 3600));
            pc.sendPackets(new S_SkillSound(objectId, 191));
            pc.broadcastPacket(new S_SkillSound(objectId, 191));
            pc.setMoveSpeed(1);
        } catch (Exception e) {
            pc.sendPackets(new S_SystemMessage(".�ӵ� ��ɾ� ����"));
        }
    }

    private void disbi(L1PcInstance pc) {
        for (L1Object obj : L1World.getInstance().getVisibleObjects(pc, 20)) {
            if (obj instanceof L1MonsterInstance) {
                L1NpcInstance npc = (L1NpcInstance) obj;
                npc.receiveDamage(pc, 200000);
                if (npc.getCurrentHp() <= 0) {
                    pc.sendPackets(new S_SkillSound(obj.getId(), 1815));
                    pc.broadcastPacket(new S_SkillSound(obj.getId(), 1815));
                } else {
                    pc.sendPackets(new S_SkillSound(obj.getId(), 1815));
                    pc.broadcastPacket(new S_SkillSound(obj.getId(), 1815));
                }
            }
        }
    }

    private void adena(L1PcInstance pc, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String para1 = stringtokenizer.nextToken();
            int count = Integer.parseInt(para1);
            L1ItemInstance adena = pc.getInventory().storeItem(L1ItemId.ADENA, count, "GM��");
            if (adena != null) {
                pc.sendPackets(new S_SystemMessage((new StringBuilder()).append(count).append("�Ƶ����� ���߽��ϴ�.").toString()));
            }
        } catch (Exception e) {
            pc.sendPackets(new S_SystemMessage((new StringBuilder()).append(".�Ƶ��� [�׼�]�� �Է� ���ּ���.").toString()));
        }
    }

    private void moveToChar(L1PcInstance gm, String pcName) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(pcName);
            if (target != null) {
                L1Teleport.teleport(gm, target.getX(), target.getY(), target.getMapId(), 5, false);
                gm.sendPackets(new S_SystemMessage((new StringBuilder()).append(pcName).append("�Կ��� �̵��߽��ϴ�.").toString()));
            } else {
                gm.sendPackets(new S_SystemMessage((new StringBuilder()).append(pcName).append("���� ����ϴ�.").toString()));
            }
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".��� [ĳ���͸�]�� �Է� ���ּ���."));
        }
    }

    private void resetTrap() {
        L1WorldTraps.getInstance().resetAllTraps();
    }

    private void hometown(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            String para1 = st.nextToken();
            if (para1.equalsIgnoreCase("����")) {
                HomeTownTimeController.getInstance().dailyProc();
            } else if (para1.equalsIgnoreCase("�Ŵ�")) {
                HomeTownTimeController.getInstance().monthlyProc();
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".ȨŸ�� [���� �Ǵ� �Ŵ�] �̶�� �Է� ���ּ���."));
        }
    }

    private void opc(L1PcInstance gm, String param) {
        try {
            gm.sendPackets(new S_OpCode_Test(Integer.parseInt(param), 0, gm));
        } catch (Exception ex) {
            try {
                gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(0, 0, gm)).getInfo()));
            } catch (Exception ex2) {
                gm.sendPackets(new S_SystemMessage("S_OpCode_Test�� ���� �߻��߽��ϴ�."));
            }
        }
    }

    private void opc1(L1PcInstance gm, String param) {
        try {
            gm.sendPackets(new S_OpCode_Test(Integer.parseInt(param), 1, gm));
        } catch (Exception ex) {
            try {
                gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(0, 1, gm)).getInfo()));
            } catch (Exception ex2) {
                gm.sendPackets(new S_SystemMessage("S_OpCode_Test�� ���� �߻��߽��ϴ�."));
            }
        }
    }

    private void opc2(L1PcInstance gm, String param) {
        try {
            gm.sendPackets(new S_OpCode_Test(Integer.parseInt(param), 2, gm));
        } catch (Exception ex) {
            try {
                gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(0, 2, gm)).getInfo()));
            } catch (Exception ex2) {
                gm.sendPackets(new S_SystemMessage("S_OpCode_Test�� ���� �߻��߽��ϴ�."));
            }
        }
    }

    private void opcId(L1PcInstance gm, String param) {
        try {
            gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(Integer.parseInt(param), 0, gm)).getCode()));
        } catch (Exception ex) {
            try {
                gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(0, 0, gm)).getCodeList()));
            } catch (Exception ex2) {
                gm.sendPackets(new S_SystemMessage("S_OpCode_Test�� ���� �߻��߽��ϴ�."));
            }
        }
    }

    private void opcId1(L1PcInstance gm, String param) {
        try {
            gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(Integer.parseInt(param), 1, gm)).getCode()));
        } catch (Exception ex) {
            try {
                gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(0, 1, gm)).getCodeList()));
            } catch (Exception ex2) {
                gm.sendPackets(new S_SystemMessage("S_OpCode_Test�� ���� �߻��߽��ϴ�."));
            }
        }
    }

    private void opcId2(L1PcInstance gm, String param) {
        try {
            gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(Integer.parseInt(param), 2, gm)).getCode()));
        } catch (Exception ex) {
            try {
                gm.sendPackets(new S_SystemMessage((new S_OpCode_Test(0, 2, gm)).getCodeList()));
            } catch (Exception ex2) {
                gm.sendPackets(new S_SystemMessage("S_OpCode_Test�� ���� �߻��߽��ϴ�."));
            }
        }
    }

    private void shutdownAbort() {
        GameServer.getInstance().abortShutdown();
    }

    private void shutdownNow() {
        GameServer.getInstance().shutdown();
    }

    private void shutdown(L1PcInstance gm, String params) {
        try {
            int sec = 0;
            StringTokenizer st = new StringTokenizer(params);
            if (st.hasMoreTokens()) {
                String param1 = st.nextToken();
                sec = Integer.parseInt(param1, 10);
            }
            if (sec < 5) {
                sec = 5;
            }
            GameServer.getInstance().shutdownWithCountdown(sec);
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� �ð�(��)�� �Է��� �ּ���."));
        }
    }

    private void npcSpawn(L1PcInstance gm, String param, String type) {
        String msg = null;
        try {
            int npcid = Integer.parseInt(param.trim());
            L1Npc template = NpcTable.getInstance().getTemplate(npcid);
            if (template == null) {
                msg = "�ش��ϴ� NPC�� �߰ߵ��� �ʽ��ϴ�.";
                return;
            }
            if (type.equals("mob")) {
                if (!template.getImpl().equals("L1Monster")) {
                    msg = "������ NPC�� L1Monster�� �ƴմϴ�.";
                    return;
                }
                SpawnTable.storeSpawn(gm, template);
            } else if (type.equals("npc")) {
                NpcSpawnTable.getInstance().storeSpawn(gm, template);
            }
            mobspawn(gm, npcid, 0, false);
            msg = new StringBuilder().append(template.get_name()).append(" (" + npcid + ") ").append("�� �߰��߽��ϴ�.").toString();
        } catch (Exception e) {
            msg = ".���Ǿ� NPCID ��� �Է��� �ּ���.";
        } finally {
            if (msg != null) {
                gm.sendPackets(new S_SystemMessage(msg));
            }
        }
    }

    private void spawn(L1PcInstance gm, String param) {
        try {
            StringTokenizer tok = new StringTokenizer(param);
            String nameid = tok.nextToken();
            int count = 1;
            if (tok.hasMoreTokens()) {
                count = Integer.parseInt(tok.nextToken());
            }
            int randomrange = 0;
            if (tok.hasMoreTokens()) {
                randomrange = Integer.parseInt(tok.nextToken(), 10);
            }
            int npcid = 0;
            try {
                npcid = Integer.parseInt(nameid);
            } catch (NumberFormatException e) {
                npcid = NpcTable.getInstance().findNpcIdByNameWithoutSpace(nameid);
                if (npcid == 0) {
                    gm.sendPackets(new S_SystemMessage("�ش� NPC�� �߰ߵ��� �ʽ��ϴ�."));
                    return;
                }
            }
            spawnTF = true;
            for (int k3 = 0; k3 < count; k3++) {
                mobspawn(gm, npcid, randomrange, false);
            }
            nameid = NpcTable.getInstance().getTemplate(npcid).get_name();
            gm.sendPackets(new S_SystemMessage(nameid + "(" + npcid + ") (" + count + ")�� ��ȯ�߽��ϴ�.(����:" + randomrange + ")"));
            spawnTF = false;
        } catch (Exception e) {
            _log.log(Level.SEVERE, e.getLocalizedMessage(), e);
            gm.sendPackets(new S_SystemMessage(".���� npcid|name [��] [����] ��� �Է��� �ּ���."));
        }
    }

    private void clanrecall(L1PcInstance gm, String cmd) {
        String s = null;
        try {
            s = cmd.substring(5);
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���ͼ�ȯ �����̸����� �Է��� �ּ���."));
            return;
        }
        L1Clan clan = L1World.getInstance().getClan(s);
        if (clan != null) {
            String clan_member_name[] = clan.getAllMembers();
            try {
                int i;
                for (i = 0; i < clan_member_name.length; i++) {
                    L1PcInstance target = L1World.getInstance().getPlayer(clan_member_name[i]);
                    if (target != null) {
                        if (gm != null && target.getAccessLevel() != 200) {
                            recallnow(gm, target);
                        }
                    }
                }
            } catch (Exception e) {
                gm.sendPackets(new S_SystemMessage(".���ͼ�ȯ ��ɾ� ����"));
                return;
            }
        } else {
            gm.sendPackets(new S_SystemMessage(s + "�� ������ �������� �ʽ��ϴ�."));
        }
    }

    private void changeWeather(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String s27 = stringtokenizer.nextToken();
            int weather = Integer.parseInt(s27);
            L1World world = L1World.getInstance();
            world.setWeather(weather);
            L1World.getInstance().broadcastPacketToAll(new S_Weather(weather));
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� 0~3, 16~19 ��� �Է��� �ּ���."));
        }
    }

    private void visible(L1PcInstance gm) {
        try {
            gm.setGmInvis(false);
            gm.sendPackets(new S_Invis(gm.getId(), 0));
            L1World.getInstance().broadcastPacketToAll(new S_Invis(gm.getId(), 0));
            gm.sendPackets(new S_SystemMessage("������¸� �����߽��ϴ�."));
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".������ ��ɾ� ����"));
        }
    }

    private void invisible(L1PcInstance gm) {
        try {
            gm.setGmInvis(true);
            gm.sendPackets(new S_Invis(gm.getId(), 1));
            L1World.getInstance().broadcastPacketToAll(new S_Invis(gm.getId(), 1));
            gm.sendPackets(new S_SystemMessage("������°� �Ǿ���ϴ�."));
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� ��ɾ� ����"));
        }
    }

    private void recall(L1PcInstance gm, String pcName) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(pcName);
            if (target != null) {
                recallnow(gm, target);
            } else {
                gm.sendPackets(new S_SystemMessage("�׷��� ĳ���ʹ� ����ϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".��ȯ ĳ���͸����� �Է��� �ּ���."));
        }
    }

    private void allrecall(L1PcInstance gm) {
        try {
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                if (!pc.isGm()) {
                    recallnow(gm, pc);
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".��ü��ȯ ��ɾ� ����"));
        }
    }

    private void partyrecall(L1PcInstance pc, String pcName) {
        L1PcInstance target = L1World.getInstance().getPlayer(pcName);
        if (target != null) {
            L1Party party = target.getParty();
            if (party != null) {
                int x = pc.getX();
                int y = pc.getY() + 2;
                short map = pc.getMapId();
                L1PcInstance[] players = party.getMembers();
                for (L1PcInstance pc2 : players) {
                    try {
                        L1Teleport.teleport(pc2, x, y, map, 5, true);
                        pc2.sendPackets(new S_SystemMessage("���� �����Ϳ��� ��ȯ�Ǿ���ϴ�."));
                    } catch (Exception e) {
                        _log.log(Level.SEVERE, "", e);
                    }
                }
            } else {
                pc.sendPackets(new S_SystemMessage("��Ƽ ����� �ƴմϴ�."));
            }
        } else {
            pc.sendPackets(new S_SystemMessage("�׷��� ĳ���ʹ� ����ϴ�."));
        }
    }

    private void recallnow(L1PcInstance gm, L1PcInstance target) {
        try {
            L1Teleport.teleportToTargetFront(target, gm, 2);
            gm.sendPackets(new S_SystemMessage((new StringBuilder()).append(target.getName()).append("���� ��ȯ�߽��ϴ�.").toString()));
            target.sendPackets(new S_SystemMessage("���� �����Ϳ��� ��ȯ�Ǿ���ϴ�."));
        } catch (Exception e) {
            _log.log(Level.SEVERE, "", e);
        }
    }

    private void polymorph(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            String name = st.nextToken();
            int polyid = Integer.parseInt(st.nextToken());
            L1PcInstance pc = L1World.getInstance().getPlayer(name);
            if (pc == null) {
                gm.sendPackets(new S_ServerMessage(73, name));
            } else {
                try {
                    L1PolyMorph.doPoly(pc, polyid, 7200, L1PolyMorph.MORPH_BY_GM);
                } catch (Exception exception) {
                    gm.sendPackets(new S_SystemMessage(".���� ĳ���͸� �׷���ID ��� �Է��� �ּ���."));
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� ĳ���͸� �׷���ID ��� �Է��� �ּ���."));
        }
    }

    private void chatng(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            String name = st.nextToken();
            int time = Integer.parseInt(st.nextToken());
            L1PcInstance pc = L1World.getInstance().getPlayer(name);
            if (pc != null) {
                pc.setSkillEffect(L1SkillId.STATUS_CHAT_PROHIBITED, time * 60 * 1000);
                pc.sendPackets(new S_SkillIconGFX(36, time * 60));
                pc.sendPackets(new S_ServerMessage(286, String.valueOf(time)));
                gm.sendPackets(new S_ServerMessage(287, name));
                L1World.getInstance().broadcastPacketToAll(new S_SystemMessage("" + name + "���� ���� ä�ñ��� ���Դϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".ä�� ĳ���͸� �ð�(��)�̶�� �Է��� �ּ���."));
        }
    }

    private void chat(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            if (st.hasMoreTokens()) {
                String flag = st.nextToken();
                String msg;
                if (flag.compareToIgnoreCase("��") == 0) {
                    L1World.getInstance().set_worldChatElabled(true);
                    msg = "��ü ä���� �����ϰ� �߽��ϴ�.";
                } else if (flag.compareToIgnoreCase("��") == 0) {
                    L1World.getInstance().set_worldChatElabled(false);
                    msg = "��ü ä���� �����߽��ϴ�.";
                } else {
                    throw new Exception();
                }
                gm.sendPackets(new S_SystemMessage(msg));
            } else {
                String msg;
                if (L1World.getInstance().isWorldChatElabled()) {
                    msg = "���� ��ü ä���� �����մϴ�. ä�� �� ���� ������ �� �ֽ��ϴ�.";
                } else {
                    msg = "���� ��ü ä���� �����Ǿ� �ֽ��ϴ�. ä�� �� ���� �����ϰ� �� �� �ֽ��ϴ�.";
                }
                gm.sendPackets(new S_SystemMessage(msg));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".ä�� �� �Ǵ� ������ �Է����ּ���."));
        }
    }

    private void teleportTo(L1PcInstance pc, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            int locx = Integer.parseInt(st.nextToken());
            int locy = Integer.parseInt(st.nextToken());
            short mapid;
            if (st.hasMoreTokens()) {
                mapid = Short.parseShort(st.nextToken());
            } else {
                mapid = pc.getMapId();
            }
            L1Teleport.teleport(pc, locx, locy, mapid, 5, false);
            pc.sendPackets(new S_SystemMessage("��ǥ " + locx + ", " + locy + ", " + mapid + "�� �̵��߽��ϴ�."));
        } catch (Exception e) {
            pc.sendPackets(new S_SystemMessage(".�̵� X��ǥ Y��ǥ [�� ID] ��� �Է��� �ּ���."));
        }
    }

    private int _spawnId = 0;

    /** GM��ɾ�.tospawn �κ��� �Ҹ���.������ spawnid�� ��ǥ�� ����. */
    private void tospawn(L1PcInstance gm, String param) {
        try {
            if (param.isEmpty() || param.equals("+")) {
                _spawnId++;
            } else if (param.equals("-")) {
                _spawnId--;
            } else {
                StringTokenizer st = new StringTokenizer(param);
                _spawnId = Integer.parseInt(st.nextToken());
            }
            L1Spawn spawn = NpcSpawnTable.getInstance().getTemplate(_spawnId);
            if (spawn == null) {
                spawn = SpawnTable.getInstance().getTemplate(_spawnId);
            }
            if (spawn != null) {
                L1Teleport.teleport(gm, spawn.getLocX(), spawn.getLocY(), spawn.getMapId(), 5, false);
                gm.sendPackets(new S_SystemMessage("spawnid(" + _spawnId + ")�� ��� ���ϴ�"));
            } else {
                gm.sendPackets(new S_SystemMessage("spawnid(" + _spawnId + ")(��)�� �߰ߵ��� �ʽ��ϴ�"));
            }
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage("Error    usage:.tospawn spawnid|+|-"));
        }
    }

    private void makeItemSet(L1PcInstance gm, String param) {
        try {
            String name = new StringTokenizer(param).nextToken();
            List<ItemSetItem> list = GMCommandsConfig.ITEM_SETS.get(name);
            if (list == null) {
                gm.sendPackets(new S_SystemMessage(name + " �����ǵ� ��Ʈ�Դϴ�"));
                return;
            }
            for (ItemSetItem item : list) {
                L1Item temp = ItemTable.getInstance().getTemplate(item.getId());
                if (!temp.isStackable() && 0 != item.getEnchant()) {
                    for (int i = 0; i < item.getAmount(); i++) {
                        L1ItemInstance inst = ItemTable.getInstance().createItem(item.getId());
                        inst.setEnchantLevel(item.getEnchant());
                        gm.getInventory().storeItem(inst, "GM��");
                    }
                } else {
                    gm.getInventory().storeItem(item.getId(), item.getAmount(), "GM��");
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�ۼ�Ʈ ��Ʈ������ �Է��� �ּ���."));
        }
    }

    private void givesItem(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            String nameid = st.nextToken();
            int count = 1;
            if (st.hasMoreTokens()) {
                count = Integer.parseInt(st.nextToken());
            }
            int enchant = 0;
            if (st.hasMoreTokens()) {
                enchant = Integer.parseInt(st.nextToken());
            }
            int isId = 0;
            if (st.hasMoreTokens()) {
                isId = Integer.parseInt(st.nextToken());
            }
            int itemid = 0;
            try {
                itemid = Integer.parseInt(nameid);
            } catch (NumberFormatException e) {
                itemid = ItemTable.getInstance().findItemIdByNameWithoutSpace(nameid);
                if (itemid == 0) {
                    gm.sendPackets(new S_SystemMessage("�ش� �������� �߰ߵ��� �ʾҽ��ϴ�."));
                    return;
                }
            }
            L1Item temp = ItemTable.getInstance().getTemplate(itemid);
            if (temp != null) {
                if (temp.isStackable()) {
                    L1ItemInstance item = ItemTable.getInstance().createItem(itemid);
                    item.setEnchantLevel(0);
                    item.setCount(count);
                    if (isId == 1) {
                        item.setIdentified(true);
                    }
                    if (gm.getInventory().checkAddItem(item, count) == L1Inventory.OK) {
                        gm.getInventory().storeItem(item, "GM��");
                        gm.sendPackets(new S_ServerMessage(403, item.getLogName() + "(ID:" + itemid + ")"));
                    }
                } else {
                    L1ItemInstance item = null;
                    int createCount;
                    for (createCount = 0; createCount < count; createCount++) {
                        item = ItemTable.getInstance().createItem(itemid);
                        item.setEnchantLevel(enchant);
                        if (isId == 1) {
                            item.setIdentified(true);
                        }
                        if (gm.getInventory().checkAddItem(item, 1) == L1Inventory.OK) {
                            gm.getInventory().storeItem(item, "GM��");
                        } else {
                            break;
                        }
                    }
                    if (createCount > 0) {
                        gm.sendPackets(new S_ServerMessage(403, item.getLogName() + "(ID:" + itemid + ")"));
                    }
                }
            } else {
                gm.sendPackets(new S_SystemMessage("���� ID�� �������� �������� �ʽ��ϴ�"));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".������ [itemid �Ǵ� name] [����] [��æƮ��] [���� ����] ��� �Է��� �ּ���."));
        }
    }

    private void present(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            String account = st.nextToken();
            int itemid = Integer.parseInt(st.nextToken(), 10);
            int enchant = Integer.parseInt(st.nextToken(), 10);
            int count = Integer.parseInt(st.nextToken(), 10);
            L1Item temp = ItemTable.getInstance().getTemplate(itemid);
            if (temp == null) {
                gm.sendPackets(new S_SystemMessage("�������� �ʴ� ������ ID�Դϴ�."));
                return;
            }
            L1DwarfInventory.present(account, itemid, enchant, count);
            gm.sendPackets(new S_SystemMessage(temp.getNameId() + "��" + count + "�� ���� �߽��ϴ�.", true));
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� [������] [������ ID] [��æƮ��] [�����ۼ�]�� �Է� ���ּ���.(�������� *���� �ϸ� ��ü ����)"));
        }
    }

    private void lvPresent(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            int minlvl = Integer.parseInt(st.nextToken(), 10);
            int maxlvl = Integer.parseInt(st.nextToken(), 10);
            int itemid = Integer.parseInt(st.nextToken(), 10);
            int enchant = Integer.parseInt(st.nextToken(), 10);
            int count = Integer.parseInt(st.nextToken(), 10);
            L1Item temp = ItemTable.getInstance().getTemplate(itemid);
            if (temp == null) {
                gm.sendPackets(new S_SystemMessage("�������� �ʴ� ������ ID�Դϴ�."));
                return;
            }
            L1DwarfInventory.present(minlvl, maxlvl, itemid, enchant, count);
            l1j.server.Leaf.tarea.append("\r\n[�ý���] " + temp.getName() + "��" + count + "�� ���� �߽��ϴ�.(Lv" + minlvl + "~" + maxlvl + ")");
            gm.sendPackets(new S_SystemMessage(temp.getName() + "��" + count + "�� ���� �߽��ϴ�.(Lv" + minlvl + "~" + maxlvl + ")"));
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�������� minlvl maxlvl ������ID ��æƮ�� �����ۼ��� �Է��� �ּ���."));
        }
    }

    private void kill(L1PcInstance gm, String pcName) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(pcName);
            if (target != null) {
                target.setCurrentHp(0);
                target.death(null);
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�׾�� ĳ���͸����� �Է��� �ּ���."));
        }
    }

    private void ress(L1PcInstance gm) {
        try {
            int objid = gm.getId();
            gm.sendPackets(new S_SkillSound(objid, 759));
            gm.broadcastPacket(new S_SkillSound(objid, 759));
            gm.setCurrentHp(gm.getMaxHp());
            gm.setCurrentMp(gm.getMaxMp());
            for (L1PcInstance pc : L1World.getInstance().getVisiblePlayer(gm)) {
                if (pc.getCurrentHp() == 0 && pc.isDead()) {
                    pc.sendPackets(new S_SystemMessage("��ڿ� ���� �һ��� �޾ҽ��ϴ�."));
                    pc.broadcastPacket(new S_SkillSound(pc.getId(), 3944));
                    pc.sendPackets(new S_SkillSound(pc.getId(), 3944));
                    pc.setTempID(objid);
                    pc.sendPackets(new S_Message_YN(322, ""));
                } else {
                    pc.sendPackets(new S_SystemMessage("��ڰ� �޷� �־���ϴ�."));
                    pc.broadcastPacket(new S_SkillSound(pc.getId(), 832));
                    pc.sendPackets(new S_SkillSound(pc.getId(), 832));
                    pc.setCurrentHp(pc.getMaxHp());
                    pc.setCurrentMp(pc.getMaxMp());
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".��Ȱ ��ɾ� ����"));
        }
    }

    private void gmRoom(L1PcInstance gm, String room) {
        try {
            int i = 0;
            try {
                i = Integer.parseInt(room);
            } catch (NumberFormatException e) {
            }
            if (i == 1) {
                L1Teleport.teleport(gm, 32737, 32796, (short) 99, 5, false);
            } else if (i == 2) {
                L1Teleport.teleport(gm, 32644, 32955, (short) 0, 5, false);
            } else if (i == 3) {
                L1Teleport.teleport(gm, 33429, 32814, (short) 4, 5, false);
            } else if (i == 4) {
                L1Teleport.teleport(gm, 32535, 32955, (short) 777, 5, false);
            } else if (i == 5) {
                L1Teleport.teleport(gm, 32736, 32787, (short) 15, 5, false);
            } else if (i == 6) {
                L1Teleport.teleport(gm, 32735, 32788, (short) 29, 5, false);
            } else if (i == 7) {
                L1Teleport.teleport(gm, 32572, 32826, (short) 64, 5, false);
            } else if (i == 8) {
                L1Teleport.teleport(gm, 32730, 32802, (short) 52, 5, false);
            } else if (i == 9) {
                L1Teleport.teleport(gm, 32895, 32533, (short) 300, 5, false);
            } else if (i == 10) {
                L1Teleport.teleport(gm, 32736, 32799, (short) 39, 5, false);
            } else if (i == 11) {
                L1Teleport.teleport(gm, 32737, 32737, (short) 8014, 5, false);
            } else if (i == 12) {
                L1Teleport.teleport(gm, 32737, 32799, (short) 8013, 5, false);
            } else if (i == 13) {
                L1Teleport.teleport(gm, 32738, 32797, (short) 509, 5, false);
            } else if (i == 14) {
                L1Teleport.teleport(gm, 32866, 32640, (short) 501, 5, false);
            } else if (i == 15) {
                L1Teleport.teleport(gm, 32603, 32766, (short) 506, 5, false);
            } else if (i == 16) {
                L1Teleport.teleport(gm, 32769, 32827, (short) 610, 5, false);
            } else {
                L1Location loc = GMCommandsConfig.ROOMS.get(room.toLowerCase());
                if (loc == null) {
                    gm.sendPackets(new S_SystemMessage(".1��ڹ�   2�ǵ���   3���   4����(�׽�)  5ĵƮ��"));
                    gm.sendPackets(new S_SystemMessage(".6���ٿ�强 7���̳׼� 8����� 9�Ƶ� 10 ���� 11â��"));
                    gm.sendPackets(new S_SystemMessage(".12������ 13ī�������� 14��ź�Ǵ� 15����̳�����   "));
                    gm.sendPackets(new S_SystemMessage(".16����   "));
                    return;
                }
                L1Teleport.teleport(gm, loc.getX(), loc.getY(), (short) loc.getMapId(), 5, false);
            }
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".��ȯ [1~16] �Ǵ� .��ȯ [��Ҹ�]�� �Է� ���ּ���.(��Ҹ��� GMCommands.xml�� ����)"));
        }
    }

    private void kick(L1PcInstance gm, String param) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(param);
            if (target != null) {
                gm.sendPackets(new S_SystemMessage((new StringBuilder()).append(target.getName()).append("���� �߹� �߽��ϴ�.").toString()));
                target.sendPackets(new S_Disconnect());
            } else {
                gm.sendPackets(new S_SystemMessage("�׷��� �̸��� ĳ���ʹ� ��峻���� �������� �ʽ��ϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�߹� ĳ���͸����� �Է��� �ּ���."));
        }
    }

    private void skick(L1PcInstance gm, String pcName) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(pcName);
            if (target != null) {
                gm.sendPackets(new S_SystemMessage((new StringBuilder()).append(target.getName()).append("���� �߹� �߽��ϴ�.").toString()));
                target.setX(33080);
                target.setY(33392);
                target.setMap((short) 4);
                target.sendPackets(new S_Disconnect());
                ClientThread targetClient = target.getNetConnection();
                targetClient.kick();
                _log.warning("GM�� �߹��ɿ� ����(" + targetClient.getAccountName() + ":" + targetClient.getHostname() + ")���� ������ ���� ���� �߽��ϴ�.");
            } else {
                gm.sendPackets(new S_SystemMessage("�׷��� �̸��� ĳ���ʹ� ��峻���� �������� �ʽ��ϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�߹� ĳ���͸����� �Է��� �ּ���."));
        }
    }

    private void powerkick(L1PcInstance gm, String pcName) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(pcName);
            IpTable iptable = IpTable.getInstance();
            if (target != null) {
                Account.ban(target.getAccountName());
                iptable.banIp(target.getNetConnection().getIp());
                L1World.getInstance().broadcastPacketToAll(new S_SystemMessage((new StringBuilder()).append(target.getName()).append(" ���� �߹� �߽��ϴ�.").toString()));
                target.sendPackets(new S_Disconnect());
            } else {
                gm.sendPackets(new S_SystemMessage("�׷��� �̸��� ĳ���ʹ� ��峻���� �������� �ʽ��ϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�����߹� ĳ���͸����� �Է��� �ּ���."));
        }
    }

    private void accbankick(L1PcInstance gm, String param) {
        try {
            L1PcInstance target = L1World.getInstance().getPlayer(param);
            if (target != null) {
                Account.ban(target.getAccountName());
                gm.sendPackets(new S_SystemMessage(target.getName() + "���� �߹� �߽��ϴ�."));
                target.sendPackets(new S_Disconnect());
            } else {
                gm.sendPackets(new S_SystemMessage("�׷��� �̸��� ĳ���ʹ� ��峻���� �������� �ʽ��ϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�����߹� ĳ���͸����� �Է��� �ּ���."));
        }
    }

    private void burf(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            int sprid = Integer.parseInt(stringtokenizer.nextToken());
            gm.sendPackets(new S_SkillSound(gm.getId(), sprid));
            gm.broadcastPacket(new S_SkillSound(gm.getId(), sprid));
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� castgfx ��� �Է��� �ּ���."));
        }
    }

    private void buff(L1PcInstance gm, String args, boolean buffMe) {
        try {
            StringTokenizer tok = new StringTokenizer(args);
            int skillId = Integer.parseInt(tok.nextToken());
            int time = 0;
            if (tok.hasMoreTokens()) {
                time = Integer.parseInt(tok.nextToken());
            }
            L1Skills skill = SkillsTable.getInstance().getTemplate(skillId);
            ArrayList<L1PcInstance> players = new ArrayList<L1PcInstance>();
            if (buffMe) {
                players.add(gm);
            } else {
                players = L1World.getInstance().getVisiblePlayer(gm);
            }
            if (skill.getTarget().equals("buff")) {
                for (L1PcInstance pc : players) {
                    new L1SkillUse().handleCommands(gm, skillId, pc.getId(), pc.getX(), pc.getY(), null, time, L1SkillUse.TYPE_SPELLSC);
                }
            } else if (skill.getTarget().equals("none")) {
                for (L1PcInstance pc : players) {
                    new L1SkillUse().handleCommands(pc, skillId, pc.getId(), pc.getX(), pc.getY(), null, time, L1SkillUse.TYPE_GMBUFF);
                }
            } else {
                gm.sendPackets(new S_SystemMessage("buff���� ��ų�� �ƴմϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� skillId time ��� �Է��� �ּ���."));
        }
    }

    private void allBuff(L1PcInstance gm) {
        int[] allBuffSkill = { PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON, IMMUNE_TO_HARM, ADVANCE_SPIRIT, BRAVE_AURA, BURNING_WEAPON, IRON_SKIN, ELEMENTAL_FIRE, SOUL_OF_FLAME, CONSENTRATION, PAYTIONS, INSIGHT, DRAGON_SKIN, MOTALBODY };
        try {
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                pc.setBuffnoch(1);
                L1SkillUse l1skilluse = new L1SkillUse();
                for (int i = 0; i < allBuffSkill.length; i++) {
                    l1skilluse.handleCommands(pc, allBuffSkill[i], pc.getId(), pc.getX(), pc.getY(), null, 0, L1SkillUse.TYPE_GMBUFF);
                }
                pc.sendPackets(new S_SystemMessage("����� �Ѱ� ������ϴ�."));
                pc.setBuffnoch(0);
            }
            gm.sendPackets(new S_SystemMessage("��峻 ��� ����鿡�� �ù����� ���� �Ͽ����ϴ�."));
        } catch (Exception exception19) {
            gm.sendPackets(new S_SystemMessage(".�ù��� ����"));
        }
    }

    private void mobspawn(L1PcInstance gm, int i, int randomrange, boolean isPineWand) {
        try {
            L1Npc l1npc = NpcTable.getInstance().getTemplate(i);
            if (l1npc != null) {
                Object obj = null;
                try {
                    String s = l1npc.getImpl();
                    Constructor constructor = Class.forName("l1j.server.server.model.Instance." + s + "Instance").getConstructors()[0];
                    Object aobj[] = { l1npc };
                    L1NpcInstance npc = (L1NpcInstance) constructor.newInstance(aobj);
                    npc.setId(IdFactory.getInstance().nextId());
                    npc.setMap(gm.getMapId());
                    if (randomrange == 0) {
                        if (gm.getHeading() == 0) {
                            npc.setX(gm.getX());
                            npc.setY(gm.getY() - 1);
                        } else if (gm.getHeading() == 1) {
                            npc.setX(gm.getX() + 1);
                            npc.setY(gm.getY() - 1);
                        } else if (gm.getHeading() == 2) {
                            npc.setX(gm.getX() + 1);
                            npc.setY(gm.getY());
                        } else if (gm.getHeading() == 3) {
                            npc.setX(gm.getX() + 1);
                            npc.setY(gm.getY() + 1);
                        } else if (gm.getHeading() == 4) {
                            npc.setX(gm.getX());
                            npc.setY(gm.getY() + 1);
                        } else if (gm.getHeading() == 5) {
                            npc.setX(gm.getX() - 1);
                            npc.setY(gm.getY() + 1);
                        } else if (gm.getHeading() == 6) {
                            npc.setX(gm.getX() - 1);
                            npc.setY(gm.getY());
                        } else if (gm.getHeading() == 7) {
                            npc.setX(gm.getX() - 1);
                            npc.setY(gm.getY() - 1);
                        }
                    } else {
                        int tryCount = 0;
                        do {
                            tryCount++;
                            npc.setX(gm.getX() + (int) (Math.random() * randomrange) - (int) (Math.random() * randomrange));
                            npc.setY(gm.getY() + (int) (Math.random() * randomrange) - (int) (Math.random() * randomrange));
                            if (npc.getMap().isInMap(npc.getLocation()) && npc.getMap().isPassable(npc.getLocation())) {
                                break;
                            }
                            Thread.sleep(1);
                        } while (tryCount < 50);
                        if (tryCount >= 50) {
                            if (gm.getHeading() == 0) {
                                npc.setX(gm.getX());
                                npc.setY(gm.getY() - 1);
                            } else if (gm.getHeading() == 1) {
                                npc.setX(gm.getX() + 1);
                                npc.setY(gm.getY() - 1);
                            } else if (gm.getHeading() == 2) {
                                npc.setX(gm.getX() + 1);
                                npc.setY(gm.getY());
                            } else if (gm.getHeading() == 3) {
                                npc.setX(gm.getX() + 1);
                                npc.setY(gm.getY() + 1);
                            } else if (gm.getHeading() == 4) {
                                npc.setX(gm.getX());
                                npc.setY(gm.getY() + 1);
                            } else if (gm.getHeading() == 5) {
                                npc.setX(gm.getX() - 1);
                                npc.setY(gm.getY() + 1);
                            } else if (gm.getHeading() == 6) {
                                npc.setX(gm.getX() - 1);
                                npc.setY(gm.getY());
                            } else if (gm.getHeading() == 7) {
                                npc.setX(gm.getX() - 1);
                                npc.setY(gm.getY() - 1);
                            }
                        }
                    }
                    npc.setHomeX(npc.getX());
                    npc.setHomeY(npc.getY());
                    npc.setHeading(gm.getHeading());
                    L1World.getInstance().storeObject(npc);
                    L1World.getInstance().addVisibleObject(npc);
                    if (spawnTF == true) {
                        L1Object object = L1World.getInstance().findObject(npc.getId());
                        L1NpcInstance newnpc = (L1NpcInstance) object;
                        newnpc.onNpcAI();
                        newnpc.turnOnOffLight();
                        newnpc.startChat(L1NpcInstance.CHAT_TIMING_APPEARANCE);
                    }
                    if (isPineWand) {
                        L1NpcDeleteTimer timer = new L1NpcDeleteTimer(npc, 300000);
                        timer.begin();
                    }
                } catch (Exception e) {
                    _log.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        } catch (Exception exception) {
        }
    }

    public void mobspawn(ClientThread client, int i, int randomrange, boolean isPineWand) {
        mobspawn(client.getActiveChar(), i, randomrange, isPineWand);
    }

    private void gfxId(L1PcInstance gm, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            int gfxid = Integer.parseInt(st.nextToken(), 10);
            int count = Integer.parseInt(st.nextToken(), 10);
            for (int i = 0; i < count; i++) {
                L1Npc l1npc = NpcTable.getInstance().getTemplate(45001);
                if (l1npc != null) {
                    String s = l1npc.getImpl();
                    Constructor constructor = Class.forName("l1j.server.server.model.Instance." + s + "Instance").getConstructors()[0];
                    Object aobj[] = { l1npc };
                    L1NpcInstance npc = (L1NpcInstance) constructor.newInstance(aobj);
                    npc.setId(IdFactory.getInstance().nextId());
                    npc.setGfxId(gfxid + i);
                    npc.setTempCharGfx(0);
                    npc.setNameId("");
                    npc.setMap(gm.getMapId());
                    npc.setX(gm.getX() + i * 2);
                    npc.setY(gm.getY() + i * 2);
                    npc.setHomeX(npc.getX());
                    npc.setHomeY(npc.getY());
                    npc.setHeading(4);
                    L1World.getInstance().storeObject(npc);
                    L1World.getInstance().addVisibleObject(npc);
                }
            }
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".���� id ������Ű�� ���� �Է��� �ּ���."));
        }
    }

    private void invGfxId(L1PcInstance pc, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            int gfxid = Integer.parseInt(st.nextToken(), 10);
            int count = Integer.parseInt(st.nextToken(), 10);
            for (int i = 0; i < count; i++) {
                L1ItemInstance item = ItemTable.getInstance().createItem(40005);
                item.getItem().setGfxId(gfxid + i);
                item.getItem().setName(String.valueOf(gfxid + i));
                pc.getInventory().storeItem(item, "GM��");
            }
        } catch (Exception exception) {
            pc.sendPackets(new S_SystemMessage(".�κ� id ������Ű�� ���� �Է��� �ּ���."));
        }
    }

    private void action(L1PcInstance pc, String param) {
        try {
            StringTokenizer st = new StringTokenizer(param);
            int actId = Integer.parseInt(st.nextToken(), 10);
            pc.sendPackets(new S_DoActionGFX(pc.getId(), actId));
        } catch (Exception exception) {
            pc.sendPackets(new S_SystemMessage(".�׼� actid ��� �Է��� �ּ���."));
        }
    }

    private void banIp(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String s1 = stringtokenizer.nextToken();
            String s2 = null;
            try {
                s2 = stringtokenizer.nextToken();
            } catch (Exception e) {
            }
            IpTable iptable = IpTable.getInstance();
            boolean isBanned = iptable.isBannedIp(s1);
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                if (s1.equals(pc.getNetConnection().getIp())) {
                    String msg = new StringBuilder().append("IP:").append(s1).append(" �� �������� �÷��̾�:").append(pc.getName()).toString();
                    gm.sendPackets(new S_SystemMessage(msg));
                }
            }
            if ("add".equals(s2) && !isBanned) {
                iptable.banIp(s1);
                String msg = new StringBuilder().append("IP:").append(s1).append(" �� BAN IP�� ����߽��ϴ�.").toString();
                gm.sendPackets(new S_SystemMessage(msg));
            } else if ("del".equals(s2) && isBanned) {
                if (iptable.liftBanIp(s1)) {
                    String msg = new StringBuilder().append("IP:").append(s1).append(" �� BAN IP�κ��� �����߽��ϴ�.").toString();
                    gm.sendPackets(new S_SystemMessage(msg));
                }
            } else {
                if (isBanned) {
                    String msg = new StringBuilder().append("IP:").append(s1).append(" �� BAN IP�� ��ϵǾ� �ֽ��ϴ�.").toString();
                    gm.sendPackets(new S_SystemMessage(msg));
                } else {
                    String msg = new StringBuilder().append("IP:").append(s1).append(" �� BAN IP�� ��ϵǾ� ���� �ʽ��ϴ�.").toString();
                    gm.sendPackets(new S_SystemMessage(msg));
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".������� IP�ּ� [ add | del ]��� �Է��� �ּ���."));
        }
    }

    private void who(L1PcInstance gm, String param) {
        try {
            Collection<L1PcInstance> players = L1World.getInstance().getAllPlayers();
            String amount = String.valueOf(players.size());
            S_WhoAmount s_whoamount = new S_WhoAmount(amount);
            gm.sendPackets(s_whoamount);
            if (param.equalsIgnoreCase("��ü")) {
                gm.sendPackets(new S_SystemMessage("-- �¶����� �÷��̾� --"));
                StringBuffer buf = new StringBuffer();
                for (L1PcInstance each : players) {
                    buf.append(each.getName());
                    buf.append(" / ");
                    if (buf.length() > 50) {
                        gm.sendPackets(new S_SystemMessage(buf.toString()));
                        buf.delete(0, buf.length() - 1);
                    }
                }
                if (buf.length() > 0) {
                    gm.sendPackets(new S_SystemMessage(buf.toString()));
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".���� �Ǵ� .���� ��ü ��� �Է��� �ּ���."));
        }
    }

    private void checkEnchant(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String para1 = stringtokenizer.nextToken();
            int enlvl = Integer.parseInt(para1);
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                List<L1ItemInstance> enchant = pc.getInventory().getItems();
                for (int j = 0; j < enchant.size(); ++j) {
                    if (enchant.get(j).getEnchantLevel() >= enlvl) gm.sendPackets(new S_SystemMessage(pc.getName() + " : " + enchant.get(j).getEnchantLevel() + enchant.get(j).getName() + " ����."));
                }
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".��æ�˻� [��æƮ ����]�� �Է� ���ּ���.(��ü �¶��� ����� �κ��丮�� ���� ��æƮ ���� �̻� �������� �˻�)"));
        }
    }

    private void checkAden(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String para1 = stringtokenizer.nextToken();
            int money = Integer.parseInt(para1);
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                L1ItemInstance adena = pc.getInventory().findItemId(40308);
                if (adena.getCount() >= money) gm.sendPackets(new S_SystemMessage(pc.getName() + " : " + adena.getCount() + " �Ƶ��� ����."));
            }
        } catch (Exception exception27) {
            gm.sendPackets(new S_SystemMessage(".�Ƶ��˻� [�׼�]�� �Է� ���ּ���."));
        }
    }

    private void chainfo(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String s = stringtokenizer.nextToken();
            gm.sendPackets(new S_Chainfo(1, s));
        } catch (Exception exception21) {
            gm.sendPackets(new S_SystemMessage(".�˻� [ĳ���͸�]�� �Է� ���ּ���."));
        }
    }

    private void patrol(L1PcInstance gm) {
        gm.sendPackets(new S_PacketBox(S_PacketBox.CALL_SOMETHING));
    }

    private void accountadd(L1PcInstance gm, String param) {
        try {
            StringTokenizer stringtokenizer = new StringTokenizer(param);
            String LoginName = stringtokenizer.nextToken();
            String password = stringtokenizer.nextToken();
            Connection con = null;
            PreparedStatement pstm = null;
            PreparedStatement pstm2 = null;
            ResultSet find = null;
            String login = null;
            String _ip = "000.000.000.000";
            String _host = "000.000.000.000";
            String _lastactive = "2008-01-01 00:00:00";
            int _normal = 0;
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement("SELECT login FROM accounts WHERE login=?");
            pstm.setString(1, LoginName);
            find = pstm.executeQuery();
            if (find.next()) {
                login = find.getString(1);
            }
            if (login == null) {
                pstm2 = con.prepareStatement("INSERT INTO accounts SET login=?,password=?,lastactive=?,access_level=?,ip=?,host=?,banned=? ");
                pstm2.setString(1, LoginName);
                pstm2.setString(2, password);
                pstm2.setString(3, _lastactive);
                pstm2.setInt(4, _normal);
                pstm2.setString(5, _ip);
                pstm2.setString(6, _host);
                pstm2.setInt(7, _normal);
                pstm2.execute();
                con.close();
                pstm.close();
                pstm2.close();
                find.close();
                gm.sendPackets(new S_SystemMessage("ID: " + LoginName + "  PW: " + password + "  ������ �Ϸ�!"));
            } else {
                con.close();
                pstm.close();
                find.close();
                gm.sendPackets(new S_SystemMessage(LoginName + " ������ ������ ���� �մϴ�"));
            }
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".�����߰� ������ ��й�ȣ ���Է����ּ���"));
        }
    }

    public static boolean isHpBarTarget(L1Object obj) {
        if (obj instanceof L1MonsterInstance) {
            return true;
        }
        if (obj instanceof L1PcInstance) {
            return true;
        }
        if (obj instanceof L1SummonInstance) {
            return true;
        }
        if (obj instanceof L1PetInstance) {
            return true;
        }
        return false;
    }

    private void hpBar(L1PcInstance gm, String param) {
        if (param.equalsIgnoreCase("��")) {
            gm.setSkillEffect(L1SkillId.GMSTATUS_HPBAR, 0);
        } else if (param.equalsIgnoreCase("��")) {
            gm.removeSkillEffect(L1SkillId.GMSTATUS_HPBAR);
            for (L1Object obj : gm.getKnownObjects()) {
                if (isHpBarTarget(obj)) {
                    gm.sendPackets(new S_HPMeter(obj.getId(), 0xFF));
                }
            }
        } else {
            gm.sendPackets(new S_SystemMessage(".�ǹ� �� �Ǵ� �� �̶�� �Է��� �ּ���."));
        }
    }

    private void reloadTraps() {
        L1WorldTraps.reloadTraps();
    }

    private void showTraps(L1PcInstance gm, String param) {
        if (param.equalsIgnoreCase("on")) {
            gm.setSkillEffect(L1SkillId.GMSTATUS_SHOWTRAPS, 0);
        } else if (param.equalsIgnoreCase("off")) {
            gm.removeSkillEffect(L1SkillId.GMSTATUS_SHOWTRAPS);
            for (L1Object obj : gm.getKnownObjects()) {
                if (obj instanceof L1TrapInstance) {
                    gm.removeKnownObject(obj);
                    gm.sendPackets(new S_RemoveObject(obj));
                }
            }
        } else {
            gm.sendPackets(new S_SystemMessage(".��Ʈ�� on|off ��� �Է��� �ּ���."));
        }
    }

    private String _lastCmd = "";

    private void redo(L1PcInstance gm, String param) {
        try {
            if (_lastCmd.isEmpty()) {
                gm.sendPackets(new S_SystemMessage("����ϰ� �ִ� ��ɾ ����ϴ�"));
                return;
            }
            if (param.isEmpty()) {
                gm.sendPackets(new S_SystemMessage("��ɾ� " + _lastCmd + " (��)�� ������մϴ�"));
                handleCommands(gm, _lastCmd);
            } else {
                StringTokenizer token = new StringTokenizer(_lastCmd);
                String cmd = token.nextToken() + " " + param;
                gm.sendPackets(new S_SystemMessage("��ɾ� " + cmd + " �� �����մϴ�."));
                handleCommands(gm, cmd);
            }
        } catch (Exception e) {
            _log.log(Level.SEVERE, e.getLocalizedMessage(), e);
            gm.sendPackets(new S_SystemMessage(".r ��ɾ� ����"));
        }
    }

    private String _faviCom = "";

    private void favorite(L1PcInstance gm, String param) {
        try {
            if (param.startsWith("set")) {
                StringTokenizer st = new StringTokenizer(param);
                st.nextToken();
                if (!st.hasMoreTokens()) {
                    gm.sendPackets(new S_SystemMessage("��ɾ �ϴ��Դϴ�."));
                    return;
                }
                StringBuilder cmd = new StringBuilder();
                String temp = st.nextToken();
                if (temp.equalsIgnoreCase("f")) {
                    gm.sendPackets(new S_SystemMessage("f �ڽ��� ����� �� ����ϴ�."));
                    return;
                }
                cmd.append(temp + " ");
                while (st.hasMoreTokens()) {
                    cmd.append(st.nextToken() + " ");
                }
                _faviCom = cmd.toString().trim();
                gm.sendPackets(new S_SystemMessage(_faviCom + " �� ����߽��ϴ�."));
            } else if (param.startsWith("show")) {
                gm.sendPackets(new S_SystemMessage("������ ��� ��ɾ�: " + _faviCom));
            } else if (_faviCom.isEmpty()) {
                gm.sendPackets(new S_SystemMessage("����ϰ� �ִ� ��ɾ ����ϴ�."));
            } else {
                StringBuilder cmd = new StringBuilder();
                StringTokenizer st = new StringTokenizer(param);
                StringTokenizer st2 = new StringTokenizer(_faviCom);
                while (st2.hasMoreTokens()) {
                    String temp = st2.nextToken();
                    if (temp.startsWith("%")) {
                        cmd.append(st.nextToken() + " ");
                    } else {
                        cmd.append(temp + " ");
                    }
                }
                while (st.hasMoreTokens()) {
                    cmd.append(st.nextToken() + " ");
                }
                gm.sendPackets(new S_SystemMessage(cmd + " �� �����մϴ�."));
                handleCommands(gm, cmd.toString());
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".f set ��ɾ� " + "| .f show | .f [�μ�] ��� �Է��� �ּ���."));
            _log.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void gm(L1PcInstance gm) {
        if (gm.isGm()) {
            gm.setGm(false);
            gm.sendPackets(new S_SystemMessage("setGm = false."));
        } else {
            gm.setGm(true);
            gm.sendPackets(new S_SystemMessage("setGm = true"));
        }
    }

    private static String encodePassword(String rawPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte buf[] = rawPassword.getBytes("UTF-8");
        buf = MessageDigest.getInstance("SHA").digest(buf);
        return Base64.encodeBytes(buf);
    }

    private void to_Change_Passwd(L1PcInstance gm, L1PcInstance pc, String passwd) {
        try {
            String login = null;
            String password = null;
            java.sql.Connection con = null;
            con = L1DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = null;
            PreparedStatement pstm = null;
            password = passwd;
            statement = con.prepareStatement("select account_name from characters where char_name Like '" + pc.getName() + "'");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                login = rs.getString(1);
                pstm = con.prepareStatement("UPDATE accounts SET password=? WHERE login Like '" + login + "'");
                pstm.setString(1, password);
                pstm.execute();
                gm.sendPackets(new S_SystemMessage("-��ȣ ��������- Account:[" + login + "] Password:[" + passwd + "]"));
                gm.sendPackets(new S_SystemMessage(pc.getName() + "�� ��ȣ ������ ���������� �Ϸ�Ǿ���ϴ�."));
                pc.sendPackets(new S_SystemMessage("������ ���� ������ ���� �Ǿ���ϴ�."));
            }
            rs.close();
            pstm.close();
            statement.close();
            con.close();
        } catch (Exception e) {
        }
    }

    private static boolean isDisitAlpha(String str) {
        boolean check = true;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i)) && Character.isLetterOrDigit(str.charAt(i)) && !Character.isUpperCase(str.charAt(i)) && !Character.isLowerCase(str.charAt(i))) {
                check = false;
                break;
            }
        }
        return check;
    }

    private void changePassword(L1PcInstance gm, String param) {
        try {
            StringTokenizer tok = new StringTokenizer(param);
            String user = tok.nextToken();
            String passwd = tok.nextToken();
            if (passwd.length() < 4) {
                gm.sendPackets(new S_SystemMessage("�Է��Ͻ� ��ȣ�� �ڸ����� �ʹ� ª���ϴ�."));
                gm.sendPackets(new S_SystemMessage("�ּ� 4�� �̻� �Է��� �ֽʽÿ�."));
                return;
            }
            if (passwd.length() > 12) {
                gm.sendPackets(new S_SystemMessage("�Է��Ͻ� ��ȣ�� �ڸ����� �ʹ� ��ϴ�."));
                gm.sendPackets(new S_SystemMessage("�ִ� 12�� ���Ϸ� �Է��� �ֽʽÿ�."));
                return;
            }
            if (isDisitAlpha(passwd) == false) {
                gm.sendPackets(new S_SystemMessage("��ȣ�� ������ �ʴ� ���ڰ� ���� �Ǿ� �ֽ��ϴ�."));
                return;
            }
            L1PcInstance target = L1World.getInstance().getPlayer(user);
            if (target != null) {
                to_Change_Passwd(gm, target, passwd);
            } else {
                gm.sendPackets(new S_SystemMessage("�׷� �̸��� ���� ĳ���ʹ� ����ϴ�."));
            }
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".��ȣ���� [ĳ���͸�] [������ ��ȣ]�� �Է� ���ּ���."));
        }
    }

    private void question(L1PcInstance gm, String pcName) {
        try {
            Config.setParameterValue1("Yes", 0);
            Config.setParameterValue1("No", 0);
            L1World.getInstance().broadcastPacketToAll(new S_Message_YN(622, pcName));
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".���� [����]�� �Է� ���ּ���."));
        }
    }

    private void result(L1PcInstance gm, String pcName) {
        try {
            int a = Config.Quest_Yes - Config.Quest_No;
            String b;
            if (a > 0) {
                b = "��ǥ��� *��*";
            } else if (a < 0) {
                b = "��ǥ��� *�ݴ�*";
            } else if (a == 0) {
                b = "��ǥ��� *����*";
            } else {
                b = "��ǥ��� *������*";
            }
            L1World.getInstance().broadcastPacketToAll(new S_SystemMessage("��: " + Config.Quest_Yes + "�� �ݴ�:" + Config.Quest_No + "��  " + b));
        } catch (Exception exception) {
            gm.sendPackets(new S_SystemMessage(".��� �� �Է� ���ּ���."));
        }
    }

    private void searchDatabase(L1PcInstance gm, String param) {
        try {
            StringTokenizer tok = new StringTokenizer(param);
            int type = Integer.parseInt(tok.nextToken());
            String name = tok.nextToken();
            searchObject(gm, type, name);
        } catch (Exception e) {
            gm.sendPackets(new S_SystemMessage(".�˻� [0~4] [name]�� �Է� ���ּ���."));
            gm.sendPackets(new S_SystemMessage("0=��Ÿ��, 1=����, 2=��, 3=NPC, 4=����"));
            gm.sendPackets(new S_SystemMessage("name�� ��Ȯ�� �𸣰ų� ������� �Ǿ��ִ� ����"));
            gm.sendPackets(new S_SystemMessage("'%'�� ���̳� �ڿ� �ٿ� ���ʽÿ�."));
        }
    }

    private void searchObject(L1PcInstance gm, int type, String name) {
        try {
            String str1 = null;
            String str2 = null;
            int count = 0;
            java.sql.Connection con = null;
            con = L1DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = null;
            switch(type) {
                case 0:
                    statement = con.prepareStatement("select item_id, name from etcitem where name Like '" + name + "'");
                    break;
                case 1:
                    statement = con.prepareStatement("select item_id, name from weapon where name Like '" + name + "'");
                    break;
                case 2:
                    statement = con.prepareStatement("select item_id, name from armor where name Like '" + name + "'");
                    break;
                case 3:
                    statement = con.prepareStatement("select npcid, name from npc where name Like '" + name + "'");
                    break;
                case 4:
                    statement = con.prepareStatement("select gfxid, name from npc where name Like '" + name + "'");
                    break;
                default:
                    break;
            }
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                str1 = rs.getString(1);
                str2 = rs.getString(2);
                gm.sendPackets(new S_SystemMessage("id : [" + str1 + "], name : [" + str2 + "]"));
                count++;
            }
            rs.close();
            statement.close();
            con.close();
            gm.sendPackets(new S_SystemMessage("�� [" + count + "]���� �����Ͱ� �˻��Ǿ���ϴ�."));
        } catch (Exception e) {
        }
    }

    private void PetRace(L1PcInstance gm) {
        try {
            L1PetRace pe11 = new L1PetRace();
            if (!pe11.isStartGame()) {
                pe11.start(1);
                L1World.getInstance().setPetRace(pe11);
            } else {
                gm.sendPackets(new S_SystemMessage("�̹� �� ���̽��� �����߽��ϴ�."));
            }
        } catch (Exception e) {
        }
    }
}
