package net.virtuemart.www.possync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.CharacterCodingException;
import java.util.logging.Level;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.util.AltEncrypter;
import com.openbravo.pos.util.Base64Encoder;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import net.virtuemart.www.VM_Categories.AddCategoryInput;
import net.virtuemart.www.VM_Categories.Categorie;
import net.virtuemart.www.VM_Categories.GetAllCategoriesInput;
import net.virtuemart.www.VM_Categories.VM_CategoriesProxy;
import net.virtuemart.www.VM_Order.CreateOrderInput;
import net.virtuemart.www.VM_Order.Order;
import net.virtuemart.www.VM_Order.OrderRequest;
import net.virtuemart.www.VM_Order.ReturnOutput;
import net.virtuemart.www.VM_Order.VM_OrderProxy;
import net.virtuemart.www.VM_Product.GetAllProductsInput;
import net.virtuemart.www.VM_Product.Produit;
import net.virtuemart.www.VM_Product.Tax;
import net.virtuemart.www.VM_Product.UpdateProductInput;
import net.virtuemart.www.VM_Product.VM_ProductProxy;
import net.virtuemart.www.VM_SQLQueries.ColumnAndValue;
import net.virtuemart.www.VM_SQLQueries.SQLRequest;
import net.virtuemart.www.VM_SQLQueries.SQLResult;
import net.virtuemart.www.VM_SQLQueries.VM_SQLQueriesProxy;
import net.virtuemart.www.VM_Tools.LoginInfo;
import net.virtuemart.www.VM_Users.AddUserInput;
import net.virtuemart.www.VM_Users.AddVendorInput;
import net.virtuemart.www.VM_Users.GetUsersInput;
import net.virtuemart.www.VM_Users.User;
import net.virtuemart.www.VM_Users.VM_UsersProxy;
import net.virtuemart.www.VM_Users.Vendor;

public class ExternalSalesHelper {

    private static final String country = "BEL";

    private String wsPosid;

    private LoginInfo wsLogin;

    private String wsURL;

    private String wsPayID;

    private String hostname;

    private String vm_path = "/administrator/components/com_vm_soa/services/VM_";

    private String vm_path_end = "Service.php";

    private String CategoriesURL = vm_path + "Categories" + vm_path_end;

    private String ProductURL = vm_path + "Product" + vm_path_end;

    private String UsersURL = vm_path + "Users" + vm_path_end;

    private String OrderURL = vm_path + "Orders" + vm_path_end;

    private String QueriesURL = vm_path + "SQLQueries" + vm_path_end;

    private VM_CategoriesProxy categoriesProxy;

    private VM_UsersProxy usersProxy;

    private VM_ProductProxy productProxy;

    private VM_OrderProxy orderProxy;

    private VM_SQLQueriesProxy queriesProxy;

    private static Logger logger = Logger.getLogger("com.openbravo.data.loader.PreparedSentence");

    private CharsetEncoder encoderUTF;

    private CharsetEncoder encoderISO;

    /**
     * @param dlsystem
     * @throws BasicException
     * @throws ServiceException
     * @throws MalformedURLException
     */
    public ExternalSalesHelper(DataLogicSystem dlsystem) throws BasicException, ServiceException, MalformedURLException {
        Charset charsetUTF = Charset.forName("UTF-8");
        Charset charsetISO = Charset.forName("ISO-8859-1");
        encoderUTF = charsetUTF.newEncoder();
        encoderISO = charsetISO.newEncoder();
        wsLogin = new LoginInfo();
        wsURL = WSInfo.getWsurl();
        wsPosid = WSInfo.getWsposid();
        hostname = TicketInfo.getHostname();
        String user = WSInfo.getWsuser();
        wsLogin.setLogin(user);
        wsPayID = WSInfo.getWspayid();
        if (wsPayID == null || wsPayID.equals("")) {
            wsPayID = "1";
        }
        AltEncrypter cypher = new AltEncrypter("cypherkey" + user);
        try {
            String password = cypher.decrypt(WSInfo.getWspassword().substring(6));
            wsLogin.setPassword(password);
        } catch (NullPointerException npe) {
            throw new BasicException(AppLocal.getIntString("message.propsnotdefined"));
        }
        if (wsPosid == null || wsPosid.equals("")) {
            throw new BasicException(AppLocal.getIntString("message.propsnotdefined"));
        } else {
            if (wsURL.equals("") || wsURL == null) {
                throw new BasicException(AppLocal.getIntString("message.urlnotdefined"));
            } else {
            }
        }
        categoriesProxy = new VM_CategoriesProxy(wsURL + CategoriesURL);
        orderProxy = new VM_OrderProxy(wsURL + OrderURL);
        usersProxy = new VM_UsersProxy(wsURL + UsersURL);
        productProxy = new VM_ProductProxy(wsURL + ProductURL);
        queriesProxy = new VM_SQLQueriesProxy(wsURL + QueriesURL);
    }

    public String getWsURL() {
        return wsURL;
    }

    public String getWsPayID() {
        return wsPayID;
    }

    public boolean checkConnection() {
        int tries = 3;
        for (int i = 0; i < tries; i++) {
            try {
                final URL url = new URL(wsURL);
                final URLConnection conn = url.openConnection();
                conn.setReadTimeout(3000);
                conn.getContent();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(ExternalSalesHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.currentThread().sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(OrdersSync.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public String getWsPosid() {
        return wsPosid;
    }

    private VM_CategoriesProxy getCategoriesProxy() throws RemoteException {
        return categoriesProxy;
    }

    private VM_UsersProxy getUsersProxy() throws RemoteException {
        return usersProxy;
    }

    private VM_ProductProxy getProductProxy() throws RemoteException {
        return productProxy;
    }

    private VM_OrderProxy getOrderProxy() throws RemoteException {
        return orderProxy;
    }

    public CharsetEncoder getEncoder() {
        return encoderUTF;
    }

    public String encodeString(String toEncode) {
        return new String(toEncode.toCharArray());
    }

    /**
     * PUBLIC
     * @param produit
     * @throws RemoteException
     */
    public void addProduct(Produit produit) throws RemoteException {
        UpdateProductInput parameter = new UpdateProductInput(wsLogin, produit);
        getProductProxy().addProduct(parameter);
    }

    public User[] getUsers() throws RemoteException {
        GetUsersInput userInput = new GetUsersInput(wsLogin, "0", "1000000000000000000");
        return usersProxy.getUsers(userInput);
    }

    public User[] getUsersBySteps(int step) throws RemoteException {
        usersProxy = new VM_UsersProxy(wsURL + UsersURL);
        int bystep = 500;
        String start = String.valueOf(step * bystep);
        int fin = (step * bystep) + (bystep);
        String end = String.valueOf(fin);
        GetUsersInput userInput = new GetUsersInput(wsLogin, start, end);
        return usersProxy.getUsers(userInput);
    }

    public boolean addUser(User user) {
        AddUserInput parameters = new AddUserInput(wsLogin, user);
        try {
            getUsersProxy().addUser(parameters);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Categorie[] getCategories() throws RemoteException {
        VM_CategoriesProxy proxy = getCategoriesProxy();
        GetAllCategoriesInput gaci = new GetAllCategoriesInput(wsLogin, "Y");
        return proxy.getAllCategories(gaci);
    }

    public void addCategory(Categorie category) throws RemoteException {
        AddCategoryInput parameters = new AddCategoryInput(wsLogin, category);
        getCategoriesProxy().addCategory(parameters);
    }

    public HashMap<Double, String> getTaxes() throws RemoteException {
        HashMap<Double, String> hash = new HashMap<Double, String>();
        Tax[] taxes = getProductProxy().getAllTax(wsLogin);
        for (Tax tax : taxes) {
            if (tax.getTax_country().equals(country)) {
                hash.put(Double.valueOf(tax.getTax_rate()), tax.getTax_rate_id());
            }
        }
        return hash;
    }

    public Produit[] getProductsCatalog() throws RemoteException {
        Produit[] produits = null;
        try {
            VM_ProductProxy proxy = getProductProxy();
            GetAllProductsInput gapi = new GetAllProductsInput();
            gapi.setLoginInfo(wsLogin);
            gapi.setProduct_publish("Y");
            gapi.setLimite_end("");
            gapi.setLimite_start("");
            gapi.setWith_childs("false");
            produits = proxy.getAllProducts(gapi);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return produits;
    }

    public void checkPosID() throws RemoteException {
        Vendor[] vendors = usersProxy.getAllVendor(wsLogin);
        for (Vendor vendor : vendors) {
            if (vendor.getVendor_id() == wsPosid) {
                if (!vendor.getVendor_name().equals(hostname)) {
                    throw new RemoteException("Wrong vendor name !!");
                } else {
                    return;
                }
            } else if (vendor.getVendor_name().equals(hostname)) {
                if (!vendor.getVendor_id().equals(wsPosid)) {
                    throw new RemoteException("Wrong vendor ID, change your POS ID to " + vendor.getVendor_id());
                } else {
                    return;
                }
            }
        }
        String contact_title = "M";
        String contact_first_name = hostname;
        String vendor_store_name = hostname;
        String vendor_min_pov = "";
        String vendor_city = "";
        String vendor_address_1 = "";
        String vendor_freeshipping = "";
        String vendor_state = "";
        String vendor_full_image = "TiiTest_4c821fbdd6c01.jpg";
        String cdate = "";
        String vendor_country = "BEL";
        String contact_phone_2 = "";
        String contact_fax = "";
        String contact_email = "POS" + wsPosid + "@beyours.be";
        String vendor_zip = "";
        String vendor_url = "";
        String contact_middle_name = "";
        String vendor_store_desc = "";
        String vendor_thumb_image = "";
        String vendor_accepted_currencies = "EUR";
        String vendor_currency = "EUR";
        String vendor_category_id = "1";
        String contact_phone_1 = "";
        String vendor_phone = "";
        String contact_last_name = "";
        String vendor_date_format = "";
        String vendor_address_format = "";
        String mdate = "";
        String vendor_terms_of_service = "";
        String vendor_image_path = "";
        String vendor_address_2 = "";
        String vendor_currency_display_style = "1|&euro;|2|,||1|8";
        Vendor vendor = new Vendor(wsPosid, hostname, contact_last_name, contact_first_name, contact_middle_name, contact_title, contact_phone_1, contact_phone_2, contact_fax, contact_email, vendor_phone, vendor_address_1, vendor_address_2, vendor_city, vendor_state, vendor_country, vendor_zip, vendor_store_name, vendor_store_desc, vendor_category_id, vendor_thumb_image, vendor_full_image, vendor_currency, cdate, mdate, vendor_image_path, vendor_terms_of_service, vendor_url, vendor_min_pov, vendor_freeshipping, vendor_currency_display_style, vendor_accepted_currencies, vendor_address_format, vendor_date_format);
        AddVendorInput parameters = new AddVendorInput(wsLogin, vendor);
        usersProxy.addVendor(parameters);
        vendors = usersProxy.getAllVendor(wsLogin);
        for (Vendor v : vendors) {
            if (v.getVendor_name().equals(hostname)) {
                this.wsPosid = v.getVendor_id();
            }
        }
    }

    public String uploadOrders(CreateOrderInput orderstoupload) throws RemoteException {
        try {
            orderstoupload.setLoginInfo(wsLogin);
            System.out.println(">> " + orderstoupload.getCustomer_note());
            ReturnOutput ro = orderProxy.createOrder(orderstoupload);
            return ro.getOutputParam();
        } catch (IOException ioe) {
            return "";
        }
    }

    public int getOrderID(String note) throws RemoteException {
        String query = "SELECT order_id FROM #__{vm}_orders ";
        SQLRequest sqlr = new SQLRequest(wsLogin, query);
        try {
            SQLResult[] results = queriesProxy.executeSQLQuery(sqlr);
            if (results.length == 1) {
                ColumnAndValue[] cav = results[0].getColumnsAndValues();
                return Integer.valueOf(cav[0].getValue());
            } else {
                return 0;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RemoteException("getOrderID ");
        }
    }

    public boolean getOrderStatus(String oid) throws RemoteException {
        OrderRequest para = new OrderRequest(wsLogin, oid, "2010-01-01 00:00:00", "2110-01-01 00:00:00");
        Order ord = orderProxy.getOrder(para);
        try {
            return ord.getOrder_status().equals("S");
        } catch (java.lang.NullPointerException npe) {
            return true;
        }
    }

    public boolean setPaid(String orderID, Date date) throws RemoteException {
        if (getOrderStatus(orderID)) return true;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateR = df.format(new Date());
        if (date != null) {
            dateR = df.format(date);
        }
        String query = " INSERT INTO #__{vm}_order_history ( order_id , order_status_code , date_added , customer_notified , comments ) " + " VALUES ( '" + orderID + "', 'C', '" + dateR + "', '0', ''); ";
        String query3 = " UPDATE #__{vm}_orders SET order_status = 'C' WHERE order_id = " + orderID + ";";
        SQLRequest sqlr = new SQLRequest(wsLogin, query);
        SQLRequest sqlr3 = new SQLRequest(wsLogin, query3);
        try {
            SQLResult[] results = queriesProxy.executeSQLQuery(sqlr);
            results = queriesProxy.executeSQLQuery(sqlr3);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean setRendu(String orderID) throws RemoteException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateR = df.format(new Date());
        String query = " INSERT INTO #__{vm}_order_history ( order_id , order_status_code , date_added , customer_notified , comments ) " + " VALUES ( '" + orderID + "', 'S', '" + dateR + "', '0', ''); ";
        String query3 = " UPDATE #__{vm}_orders SET order_status = 'S' WHERE order_id = " + orderID + " AND order_status = 'C';";
        SQLRequest sqlr = new SQLRequest(wsLogin, query);
        SQLRequest sqlr3 = new SQLRequest(wsLogin, query3);
        try {
            SQLResult[] results = queriesProxy.executeSQLQuery(sqlr);
            results = queriesProxy.executeSQLQuery(sqlr3);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean updateStatus(String orderID, Date dateCreation, Date dateReturn) throws RemoteException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateR = null;
        String dateC = df.format(new Date());
        if (dateCreation != null) {
            dateC = df.format(dateCreation);
        }
        boolean returnvalue = true;
        SQLResult[] results;
        if (dateReturn != null) {
            dateR = df.format(dateReturn);
            String query = " INSERT INTO #__{vm}_order_history ( order_id , order_status_code , date_added , customer_notified , comments ) " + " VALUES ( '" + orderID + "', 'D', '" + dateR + "', '0', 'Date return'); ";
            SQLRequest sqlr = new SQLRequest(wsLogin, query);
            try {
                results = queriesProxy.executeSQLQuery(sqlr);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                returnvalue = false;
            }
        }
        String query2 = " UPDATE #__{vm}_order_history SET date_added = '" + dateC + "' WHERE order_status_code = 'P' AND order_status_history_id =" + orderID + "; ";
        String query3 = " UPDATE #__{vm}_orders SET cdate=UNIX_TIMESTAMP('" + dateC + "') WHERE order_id = " + orderID + ";";
        SQLRequest sqlr2 = new SQLRequest(wsLogin, query2);
        SQLRequest sqlr3 = new SQLRequest(wsLogin, query3);
        try {
            results = queriesProxy.executeSQLQuery(sqlr2);
            results = queriesProxy.executeSQLQuery(sqlr3);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            returnvalue = false;
        }
        return returnvalue;
    }

    private static String getPasswordHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte raw[] = md.digest(password.getBytes("UTF-8"));
            return Base64Encoder.encode(raw);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
