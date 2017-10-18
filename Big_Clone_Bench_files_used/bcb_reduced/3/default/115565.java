import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import javax.servlet.*;
import javax.servlet.http.*;
import ApiScrumClass.PersistenceMySQL;
import ApiScrumClass.Person;
import ApiScrumClass.Product;
import ApiScrumClass.ScrumManager;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import org.json.JSONObject;
import com.google.gson.Gson;

public class ScrumMasterServlet extends HttpServlet {

    private static ScrumManager scrum = new ScrumManager();

    private static PersistenceMySQL persistencia = new PersistenceMySQL();

    private static final long serialVersionUID = 8032611514671727168L;

    private static List<Product> products = new LinkedList<Product>();

    public Boolean checkPassword(String username, String password) {
        Person p = giveUser(username);
        if (p == null) {
            return false;
        }
        if (!(p.getPassword().equals(password))) {
            return false;
        }
        return true;
    }

    public Person giveUser(String username) {
        Person person;
        try {
            List<Person> listPerson = scrum.getCompleteStaffList();
            Iterator iterProducts = listPerson.iterator();
            while (iterProducts.hasNext()) {
                person = (Person) iterProducts.next();
                if (person.getName().equals(username)) return person;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Person givePerson(String id) {
        return persistencia.getPerson(Integer.parseInt(id));
    }

    public String getPerson(String id) {
        try {
            Person p = givePerson(id);
            if (p != null) return p.getName(); else return null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getProductList() {
        try {
            List<Product> list = scrum.getProductList();
            List<String> listAux = new ArrayList<String>();
            Iterator iterProducts = list.iterator();
            while (iterProducts.hasNext()) {
                listAux.add(String.valueOf(((Product) iterProducts.next()).getId()));
            }
            return listAux;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getPersons() {
        List<String> list = new ArrayList<String>();
        Person person;
        try {
            List<Person> listPerson = scrum.getCompleteStaffList();
            if (listPerson == null) return null;
            Iterator iterProducts = listPerson.iterator();
            while (iterProducts.hasNext()) {
                person = (Person) iterProducts.next();
                list.add(String.valueOf(person.getIdPerson()));
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Producto> productsss = new LinkedList<Producto>();

    private String encryptPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest encript = MessageDigest.getInstance("MD5");
        encript.update(password.getBytes());
        byte[] b = encript.digest();
        int size = b.length;
        StringBuffer h = new StringBuffer(size);
        for (int i = 0; i < size; i++) {
            h.append(b[i]);
        }
        return h.toString();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String User = request.getParameter("user");
        String Password = request.getParameter("password");
        String Product = request.getParameter("product");
        try {
            JSONObject responseObj = new JSONObject();
            List<JSONObject> productObjects = new LinkedList<JSONObject>();
            List<Product> products = scrum.getProductList();
            Gson gson = new Gson();
            for (Product product : products) {
                JSONObject productObj = new JSONObject();
                productObj.put("Description", product.getDescription());
                productObj.put("name", product.getName());
                productObj.put("Done", product.getDone());
                productObj.put("CurrentEstimation", product.getCurrentEstimation());
                productObj.put("InitialEstimation", product.getInitialEstimation());
                productObj.put("Assigned", product.getAssigned());
                productObj.put("Planned", product.getPlanned());
                productObj.put("id", product.getId());
                productObjects.add(productObj);
            }
            responseObj.put("paso", checkPassword(User, Password));
            responseObj.put("productos", productObjects);
            PrintWriter writer = response.getWriter();
            writer.write(responseObj.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}
