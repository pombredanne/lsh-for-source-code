package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import logic.Board;
import logic.Team;
import messages.GameEventDispatcher;
import messages.GameEventListener;
import messages.ChangeTurn;
import messages.GameMessage;
import messages.MatchWinner;
import messages.MovePiece;
import messages.RotatePiece;
import messages.UmountObelisk;

public class BoardGUI extends JPanel implements MouseListener, MouseMotionListener, GameEventListener, GameEventDispatcher {

    /**
	 * 
	 */
    private static final long serialVersionUID = 4164900566546611060L;

    private final int padding = 2;

    private final int cellsize = 46;

    private int boardDimX;

    private int boardDimY;

    private int boardDimXPixels;

    private int boardDimYPixels;

    private ArrayList<GameEventListener> eventListeners;

    public boolean draging = false;

    public int dragX = -1;

    public int dragY = -1;

    public int dragXEx = -1;

    public int dragYEx = -1;

    public int lastClickX = -1;

    public int lastClickY = -1;

    public int lastClickXEx = -1;

    public int lastClickYEx = -1;

    private boolean debugging = true;

    private Board board;

    private ArrayList<Integer[]> laserTrajectory;

    private boolean drawLaser = false;

    private Team currentPlayer;

    private boolean connected = false;

    private Team localPlayer = Team.NOTEAM;

    public BoardGUI(String configurationFile, GameEventListener listener) {
        addMouseListener(this);
        addMouseMotionListener(this);
        requestFocus();
        registerGameListener(listener);
        this.setOpaque(false);
        resetMatch(configurationFile);
        boardDimX = board.getBoardDimX();
        boardDimY = board.getBoardDimY();
        boardDimXPixels = boardDimX * (cellsize + 2 * padding) + padding;
        boardDimYPixels = boardDimY * (cellsize + 2 * padding) + padding;
        currentPlayer = board.getTurn();
        this.setSize(boardDimXPixels, boardDimYPixels);
        this.setMinimumSize(new Dimension(boardDimXPixels, boardDimYPixels));
        this.setMaximumSize(new Dimension(boardDimXPixels, boardDimYPixels));
        this.setPreferredSize(new Dimension(boardDimXPixels, boardDimYPixels));
        Thread repainting = new Thread(new BoardRepainter(this));
        repainting.setDaemon(true);
        repainting.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponents(g);
        clearBoard(g);
        drawAllPieces(g);
        if (drawLaser) {
            drawLaser(g, laserTrajectory);
        }
    }

    public int getBoardDimY() {
        return boardDimYPixels;
    }

    public int getBoardDimX() {
        return boardDimXPixels;
    }

    public Team getUserTurn() {
        return board.getTurn();
    }

    public Document getGameStateDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document state = null;
        try {
            builder = factory.newDocumentBuilder();
            state = builder.newDocument();
            Element root = state.createElement("board");
            Node pieceConfiguration = state.importNode(board.getPiecesOnBoard().getFirstChild(), true);
            Element firstPlayer = state.createElement("firstPlayer");
            firstPlayer.setAttribute("player", currentPlayer.toString().toLowerCase());
            state.appendChild(root);
            root.appendChild(pieceConfiguration);
            root.appendChild(firstPlayer);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (DOMException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return state;
    }

    public String getGameStateXML() throws ParserConfigurationException, TransformerException {
        Document state = getGameStateDocument();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(state);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

    public String gameState() {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            String boardStatus = getGameStateXML();
            byte[] hash = md5.digest(boardStatus.getBytes());
            String stateBoardMsg = "";
            for (int j = 0; j < hash.length; j++) {
                stateBoardMsg += Integer.toHexString(0xFF & hash[j]);
            }
            return stateBoardMsg;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerGameListener(GameEventListener listener) {
        if (eventListeners == null) eventListeners = new ArrayList<GameEventListener>();
        eventListeners.add(listener);
    }

    @Override
    public void sendEvent(GameMessage event) {
        for (GameEventListener listener : eventListeners) {
            listener.gameEvent(event);
        }
    }

    private void clearBoard(Graphics g) {
        for (int i = 0; i < boardDimX; i++) {
            drawCell(g, Color.RED, i, 0);
            drawCell(g, Color.GRAY, i, (boardDimY - 1));
        }
        drawCell(g, Color.gray, 0, 1);
        drawCell(g, Color.gray, (boardDimX - 1), 1);
        drawCell(g, Color.red, 0, boardDimY - 2);
        drawCell(g, Color.red, boardDimX - 1, boardDimY - 2);
        for (int i = 1; i < boardDimX - 1; i++) {
            drawCell(g, Color.DARK_GRAY, i, 1);
            drawCell(g, Color.DARK_GRAY, i, boardDimY - 2);
        }
        for (int i = 0; i < boardDimX; i++) {
            for (int j = 2; j < boardDimY - 2; j++) {
                drawCell(g, Color.DARK_GRAY, i, j);
            }
        }
    }

    private void drawAllPieces(Graphics g) {
        try {
            Document confDocument = board.getPiecesOnBoard();
            Element pieceConf = (Element) confDocument.getElementsByTagName("pieceConfiguration").item(0);
            NodeList pieces = pieceConf.getElementsByTagName("piece");
            for (int i = 0; i < pieces.getLength(); i++) {
                Element piece = (Element) pieces.item(i);
                String name = piece.getAttribute("type");
                Team owner = Team.NOTEAM;
                String owner_xml = piece.getAttribute("owner");
                int posx = Integer.parseInt(piece.getAttribute("posx"));
                int posy = Integer.parseInt(piece.getAttribute("posy"));
                short orientation = 0;
                if (piece.hasAttribute("orientation")) orientation = Short.parseShort(piece.getAttribute("orientation"));
                if (owner_xml.equals("red")) {
                    owner = Team.RED;
                } else if (owner_xml.equals("grey")) {
                    owner = Team.GRAY;
                } else {
                }
                if (name.equals("pyramid")) {
                    drawPyramid(g, owner, posx, posy, orientation);
                } else if (name.equals("djed")) {
                    drawDjed(g, owner, posx, posy, orientation);
                } else if (name.equals("obelisk")) {
                    drawObelisk(g, owner, posx, posy, Integer.parseInt(piece.getAttribute("mounted")));
                } else if (name.equals("pharaoh")) {
                    drawPharaoh(g, owner, posx, posy);
                } else {
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private int drawCell(Graphics g, Color c, int x, int y) {
        Color lastColor = g.getColor();
        x *= cellsize + 2 * padding;
        y *= cellsize + 2 * padding;
        g.setColor(c);
        g.fillRect(x + padding, y + padding, cellsize, cellsize);
        g.setColor(lastColor);
        return cellsize + 2 * padding;
    }

    private void drawPiece(String image, Graphics g, int x, int y, double rotateRadians) {
        try {
            BufferedImage img;
            img = ImageIO.read(new File(image));
            Graphics2D g2 = (Graphics2D) g;
            x *= cellsize + 2 * padding;
            y *= cellsize + 2 * padding;
            x += padding + cellsize / 2;
            y += padding + cellsize / 2;
            AffineTransform trns = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(rotateRadians);
            g2.drawImage(img, -(cellsize - 2 * padding) / 2, -(cellsize - 2 * padding) / 2, (cellsize - 2 * padding), (cellsize - 2 * padding), this);
            g2.setTransform(trns);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawPyramid(Graphics g, Team team, int x, int y, int orientation) {
        String image;
        switch(team) {
            case RED:
                image = "images/pyramid-red.jpg";
                break;
            case GRAY:
                image = "images/pyramid-gray.jpg";
                break;
            default:
                image = null;
                break;
        }
        double rotateRadians;
        if (orientation == 0) {
            rotateRadians = 0;
        } else if (orientation == 1) {
            rotateRadians = -Math.PI / 2;
        } else if (orientation == 2) {
            rotateRadians = Math.PI;
        } else {
            rotateRadians = Math.PI / 2;
        }
        drawPiece(image, g, x, y, rotateRadians);
    }

    private void drawPharaoh(Graphics g, Team team, int x, int y) {
        String image;
        switch(team) {
            case RED:
                image = "images/pharaoh-red.jpg";
                break;
            case GRAY:
                image = "images/pharaoh-gray.jpg";
                break;
            default:
                image = null;
                break;
        }
        drawPiece(image, g, x, y, 0);
    }

    private void drawObelisk(Graphics g, Team team, int x, int y, int mounted) {
        String image;
        switch(team) {
            case RED:
                image = "images/obelisk-red.jpg";
                break;
            case GRAY:
                image = "images/obelisk-gray.jpg";
                break;
            default:
                image = null;
                break;
        }
        drawPiece(image, g, x, y, 0);
        if (mounted > 1) {
            g.setColor(Color.BLUE);
            g.drawString(mounted + "", x * (cellsize + 2 * padding) + cellsize * 3 / 4, y * (cellsize + 2 * padding) + cellsize / 2);
        }
    }

    private void drawDjed(Graphics g, Team team, int x, int y, short orientation) {
        String image;
        switch(team) {
            case RED:
                image = "images/djed-red.jpg";
                break;
            case GRAY:
                image = "images/djed-gray.jpg";
                break;
            default:
                image = null;
                break;
        }
        double rotateRadians;
        if (orientation == 0) {
            rotateRadians = 0;
        } else {
            rotateRadians = Math.PI / 2;
        }
        drawPiece(image, g, x, y, rotateRadians);
    }

    public boolean movePiece(int x1, int y1, int x2, int y2, Team player) {
        if (board.movePiece(x1, y1, x2, y2, player)) {
            ArrayList<Integer[]> trajectory = board.pressLaser();
            if (trajectory != null) {
                laserTrajectory = trajectory;
                currentPlayer = board.passTurn();
                sendEvent(new ChangeTurn(board.getTurn()));
                drawLaser = true;
            }
            return true;
        }
        return false;
    }

    public boolean rotatePieceLeft(int x, int y, Team player) {
        if (board.rotatePieceLeft(x, y, player)) {
            ArrayList<Integer[]> trajectory = board.pressLaser();
            if (trajectory != null) {
                laserTrajectory = trajectory;
                currentPlayer = board.passTurn();
                sendEvent(new ChangeTurn(board.getTurn()));
                drawLaser = true;
            }
            return true;
        }
        return false;
    }

    public boolean rotatePieceRight(int x, int y, Team player) {
        if (board.rotatePieceRight(x, y, player)) {
            ArrayList<Integer[]> trajectory = board.pressLaser();
            if (trajectory != null) {
                laserTrajectory = trajectory;
                currentPlayer = board.passTurn();
                sendEvent(new ChangeTurn(board.getTurn()));
                drawLaser = true;
            }
            return true;
        }
        return false;
    }

    public boolean umountObelisk(int x1, int y1, int x2, int y2, Team player) {
        if (board.umountObelisk(x1, y1, x2, y2, player)) {
            ArrayList<Integer[]> trajectory = board.pressLaser();
            if (trajectory != null) {
                laserTrajectory = trajectory;
                currentPlayer = board.passTurn();
                sendEvent(new ChangeTurn(board.getTurn()));
                drawLaser = true;
            }
            return true;
        }
        return false;
    }

    private void drawLaser(Graphics g, ArrayList<Integer[]> laserTrajectory) {
        if (laserTrajectory == null) return;
        int lastLaserx = laserTrajectory.get(0)[0];
        int lastLasery = laserTrajectory.get(0)[1];
        g.setColor(Color.GREEN);
        for (Integer[] point : laserTrajectory) {
            g.drawLine(lastLaserx * (cellsize + 2 * padding) + (cellsize + padding) / 2, lastLasery * (cellsize + 2 * padding) + (cellsize + padding) / 2, point[0] * (cellsize + 2 * padding) + (cellsize + padding) / 2, point[1] * (cellsize + 2 * padding) + (cellsize + padding) / 2);
            lastLaserx = point[0];
            lastLasery = point[1];
        }
    }

    private void gameOver(MatchWinner winnerEvent) {
        laserTrajectory = winnerEvent.getLastTrajectory();
        System.out.println(laserTrajectory);
        drawLaser = true;
        sendEvent(winnerEvent);
    }

    public void resetMatch(Document configuration) {
        try {
            board.setBoard(configuration);
            sendEvent(new ChangeTurn(board.getTurn()));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetMatch(String configurationFile) {
        try {
            board = new Board(configurationFile);
            board.registerGameListener(this);
            sendEvent(new ChangeTurn(board.getTurn()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public void setLocalPlayer(Team localPlayer) {
        this.localPlayer = localPlayer;
        connected = true;
    }

    public void unsetLocalPlayer() {
        this.localPlayer = Team.NOTEAM;
        connected = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (currentPlayer != localPlayer && localPlayer != Team.NOTEAM) return;
        draging = true;
        int x = e.getX();
        int y = e.getY();
        int cellx = x / (cellsize + 2 * padding);
        int celly = y / (cellsize + 2 * padding);
        lastClickX = cellx;
        lastClickY = celly;
        lastClickXEx = x;
        lastClickYEx = y;
        if (debugging) {
            System.out.println("Click on " + x + " " + y + " Cells " + cellx + " " + celly);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (currentPlayer != localPlayer && localPlayer != Team.NOTEAM) return;
        if (draging) {
            draging = false;
            dragX = e.getX() / (cellsize + 2 * padding);
            dragY = e.getY() / (cellsize + 2 * padding);
            if (Math.abs(lastClickX - dragX) == 1 || Math.abs(lastClickY - dragY) == 1) {
            } else if (lastClickX - dragX == 0 && lastClickY - dragY == 0) {
                System.out.println("Do rotation.");
                if (e.getButton() == 1) {
                    if (rotatePieceLeft(dragX, dragY, currentPlayer)) if (connected) sendEvent(new RotatePiece(dragX, dragY, RotatePiece.LEFT, localPlayer));
                } else if (e.getButton() == 3) {
                    if (rotatePieceRight(dragX, dragY, currentPlayer)) if (connected) sendEvent(new RotatePiece(dragX, dragY, RotatePiece.RIGHT, localPlayer));
                }
                return;
            }
            if (e.getButton() == 3) {
                if (umountObelisk(lastClickX, lastClickY, dragX, dragY, localPlayer)) if (connected) sendEvent(new UmountObelisk(lastClickX, lastClickY, dragX, dragY, localPlayer));
            } else {
                if (movePiece(lastClickX, lastClickY, dragX, dragY, localPlayer)) if (connected) sendEvent(new MovePiece(lastClickX, lastClickY, dragX, dragY, localPlayer));
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentPlayer != localPlayer && localPlayer != Team.NOTEAM) return;
        draging = true;
        dragXEx = e.getX();
        dragYEx = e.getY();
        dragX = e.getX() / (cellsize + 2 * padding);
        dragY = e.getY() / (cellsize + 2 * padding);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    private class BoardRepainter implements Runnable {

        private BoardGUI board;

        public BoardRepainter(BoardGUI board) {
            this.board = board;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                board.repaint();
                if (board.drawLaser) {
                    try {
                        Thread.sleep(1000);
                        board.drawLaser = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    board.drawLaser = false;
                }
            }
        }
    }

    @Override
    public boolean gameEvent(GameMessage message) {
        if (message instanceof MatchWinner) {
            gameOver((MatchWinner) message);
            return true;
        }
        return false;
    }
}
