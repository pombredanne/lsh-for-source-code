package com.eshop.http.servlets;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.be.bo.Facade;
import com.be.bo.GlobalParameter;
import com.be.bo.UserObject;
import com.be.vo.ContactVO;
import com.be.vo.MailAddressVO;
import com.debitors.bo.BillBO;
import com.debitors.vo.BillVO;
import com.eshop.bo.Cart;
import com.eshop.bo.SessionInitializer;

public class PaymentController extends HttpServlet {

    private static final long serialVersionUID = 3197871037476611443L;

    private ServletConfig config;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.config = config;
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    ;

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("com.eshop.http.servlets.PaymentController: ");
        HttpSession session = req.getSession();
        UserObject uo = (UserObject) session.getAttribute("userObject");
        if (uo == null) {
            uo = SessionInitializer.init(session, config);
            System.out.println("com.eshop.http.servlets.PaymentController: session initialized.");
        }
        Locale currentLocale = (Locale) req.getSession().getAttribute("org.apache.struts.action.LOCALE");
        if (currentLocale == null) {
            currentLocale = new Locale("de");
        }
        ResourceBundle myResources = ResourceBundle.getBundle("resources/eshop", currentLocale);
        if (req.getParameter("action") != null && "abort".equals(req.getParameter("action"))) {
            System.out.println("Payment has been aborted. ");
            if (uo != null) {
                uo.log("com.eshop.http.servlets.PaymentController: Yellowpay abort.", GlobalParameter.logTypeYellowPay);
            }
            session.setAttribute("eshop.user.message", myResources.getString("eshop.order.process.abort"));
            resp.sendRedirect("index.jsp?forward=shop/bodyInfo.jsp");
        } else if (req.getParameter("action") != null && "start".equals(req.getParameter("action"))) {
            System.out.println("com.eshop.http.servlets.PaymentController.doPost(): start yellowPay payment");
            if (uo != null) {
                uo.log("com.eshop.http.servlets.PaymentController: Yellowpay start.", GlobalParameter.logTypeYellowPay);
            }
            int billID = bookOrder(uo, req);
            com.debitors.bo.Facade facadeDebitor = (com.debitors.bo.Facade) uo.getFacade(GlobalParameter.facadeDebitors);
            BillVO[] bills = facadeDebitor.getBillVO(billID);
            if (bills != null && bills.length > 0) {
                req.getSession().setAttribute("eshop.billVO", bills[0]);
            }
            resp.sendRedirect("index.jsp?forward=shop/bodyYellowPay.jsp");
        } else if (req.getParameter("action") != null && "success".equals(req.getParameter("action"))) {
            System.out.println("com.eshop.http.servlets.PaymentController.doPost(): Payment was successful. ");
            if (uo != null) {
                uo.log("com.eshop.http.servlets.PaymentController: Yellowpay success.", GlobalParameter.logTypeYellowPay);
            }
            session.setAttribute("eshop.user.message", myResources.getString("eshop.order.process.success"));
            Cart cart = (Cart) session.getAttribute("shopCart");
            if (cart != null) {
                session.removeAttribute("shopCart");
            }
            resp.sendRedirect("index.jsp?forward=shop/bodyInfo.jsp");
        } else if (req.getParameter("action") != null && "failed".equals(req.getParameter("action"))) {
            System.out.println("com.eshop.http.servlets.PaymentController.doPost(): Payment failed. ");
            if (uo != null) {
                uo.log("com.eshop.http.servlets.PaymentController: Yellowpay failed.", GlobalParameter.logTypeYellowPay);
            }
            session.setAttribute("eshop.user.message", myResources.getString("eshop.order.process.failed"));
            resp.sendRedirect("index.jsp?forward=shop/bodyInfo.jsp");
        } else if (req.getParameter("action") != null && "receipt".equals(req.getParameter("action"))) {
            System.out.println("com.eshop.http.servlets.PaymentController.doPost(): Payment receipt processing. ");
            uo.log("com.eshop.http.servlets.PaymentController: Yellowpay receipt.", GlobalParameter.logTypeYellowPay);
            String txtTransactionID = req.getParameter("txtTransactionID");
            String txtEp2TrxID = req.getParameter("txtEp2TrxID");
            String txtPayMet = req.getParameter("txtPayMet");
            String txtHashBack = req.getParameter("txtHashBack");
            String eshopBillID = req.getParameter("eshopBillID");
            String eshopBillNr = req.getParameter("eshopBillNr");
            String eshopCustomerID = req.getParameter("eshopCustomerID");
            String txtOrderTotal = req.getParameter("txtOrderTotal");
            StringBuffer buffer = new StringBuffer();
            buffer.append("txtTransactionID: " + txtTransactionID + "<br>\n");
            buffer.append("txtEp2TrxID: " + txtEp2TrxID + "<br>\n");
            buffer.append("txtPayMet: " + txtPayMet + "<br>\n");
            buffer.append("txtHashBack: " + txtHashBack + "<br>\n");
            buffer.append("eshopBillID: " + eshopBillID + "<br>\n");
            buffer.append("eshopBillNr: " + eshopBillNr + "<br>\n");
            buffer.append("eshopCustomerID: " + eshopCustomerID + "<br>\n");
            System.out.println(buffer.toString());
            uo.log(buffer.toString(), GlobalParameter.logTypeYellowPay);
            if (checkHashBack(uo.getFacade(), req)) {
                if (uo != null) {
                    uo.log("com.eshop.http.servlets.PaymentController: Yellowpay txtHashBack is valid: " + " [txtTransactionID:" + txtTransactionID + "]" + " [eshopBillNr:" + eshopBillNr + "]" + " [eshopBillID:" + eshopBillID + "]", GlobalParameter.logTypeYellowPay);
                }
                int billNr = Integer.parseInt(eshopBillNr);
                com.debitors.bo.Facade facadeDebitors = (com.debitors.bo.Facade) uo.getFacade(GlobalParameter.facadeDebitors);
                BillVO billVO = facadeDebitors.getBillVOByBillNumber(billNr);
                if (billVO != null) {
                    billVO.setAmountPaid(new BigDecimal(txtOrderTotal));
                    BillBO bo = new BillBO(uo.getConnectionPropertiesVO(), uo);
                    try {
                        bo.getBillDAO().updateData(billVO);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            uo.log(req.getHeader("referer"), GlobalParameter.logTypeYellowPay);
        } else {
            System.out.println("com.eshop.http.servlets.PaymentController.doPost(): No action specified. ");
            if (uo != null) {
                uo.log("com.eshop.http.servlets.PaymentController: Yellowpay no action.", GlobalParameter.logTypeYellowPay);
            }
            session.setAttribute("eshop.user.message", myResources.getString("eshop.order.process.default"));
            resp.sendRedirect("index.jsp?forward=shop/bodyInfo.jsp");
        }
    }

    private int bookOrder(UserObject uo, HttpServletRequest req) {
        ContactVO vo = (ContactVO) req.getSession().getAttribute("eshop.contactVO");
        MailAddressVO maVO = (MailAddressVO) req.getSession().getAttribute("eshop.mailAddressVO");
        com.eshop.bo.Facade facade = (com.eshop.bo.Facade) uo.getFacade(GlobalParameter.facadeEshop);
        Cart cart = (Cart) req.getSession().getAttribute("shopCart");
        int billID = facade.storeOrder(req, uo, vo, maVO, cart);
        req.getSession().removeAttribute("shopCart");
        return billID;
    }

    private boolean checkHashBack(Facade facade, HttpServletRequest req) {
        String txtTransactionID = req.getParameter("txtTransactionID");
        String txtOrderTotal = req.getParameter("txtOrderTotal");
        String txtShopId = facade.getSystemParameter(GlobalParameter.yellowPayMDMasterShopID);
        String txtArtCurrency = facade.getSystemParameter(GlobalParameter.yellowPayMDCurrency);
        String txtHashBack = req.getParameter("txtHashBack");
        String hashSeed = facade.getSystemParameter(GlobalParameter.yellowPayMDHashSeed);
        String securityValue = txtShopId + txtArtCurrency + txtOrderTotal + hashSeed + txtTransactionID;
        MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(securityValue.getBytes());
            byte[] array = digest.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            String hash = sb.toString();
            System.out.println("com.eshop.http.servlets.PaymentController.checkHashBack: " + hash + " " + txtHashBack);
            if (txtHashBack.equals(hash)) {
                return true;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }
}
