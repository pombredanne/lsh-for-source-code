package logic.managers;

import dao.ProductRatingsDAO;
import data.Parameters;
import data.Categories;
import data.Searches;
import data.Users;
import dao.SearchesDAO;
import dao.UsersDAO;
import data.Products;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import logic.comparators.UsersOrderingComparator;
import logic.Constants;

/**
 * Manager used to modify ProfileBean values
 * @author Branislav Vaclav
 */
public class ProfileManager {

    /**
	 * Verifies whether user can be logged in using provided username and password
	 *
	 * @param username username
	 * @param password password
	 * @return         true if login is correct, false otherwise
	 */
    public static boolean testLogin(String username, String password) {
        Users user = UsersDAO.getUserByUsername(username);
        if (user == null) return false;
        String hash = hashPassword(password);
        if (user.getUsrPassword().equals(hash)) return true;
        return false;
    }

    /**
	 * Verifies whether user profile can be updated with provided username for current user id
	 *
	 * @param username updated username
	 * @param userId   current user id
	 * @return         true if profile can be updated, false otherwise
	 */
    public static boolean testProfile(String username, Integer userId) {
        Users user = UsersDAO.getUserByUsername(username);
        if (user == null) return true;
        if ((userId != null) && (user.getUsrId().equals(userId))) return true;
        return false;
    }

    /**
	 * Updates user information with last logged in time to database and returns updated user
	 *
	 * @param user user for whom last logged in time is being updated
	 * @return     updated user
	 */
    public static Users updateLogin(Users user) {
        UsersDAO.updateUserLastLoginByUsername(user);
        return UsersDAO.getUserByUsername(user.getUsrUsername());
    }

    /**
	 * Creates new user profile or updates existing one to database and returns created or updated user
	 *
	 * @param user     user that is being updated
	 * @param password password that is being used for the user profile
	 * @return         updated user
	 */
    public static Users updateProfile(Users user, String password) {
        user.setUsrPassword(hashPassword(password));
        if (user.getUsrId() == null) UsersDAO.insertUser(user); else UsersDAO.updateUser(user);
        return UsersDAO.getUserByUsername(user.getUsrUsername());
    }

    /**
	 * Provides password hash
	 *
	 * @param password password
	 * @return         hashed password
	 */
    private static String hashPassword(String password) {
        try {
            byte[] HexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
            byte[] hashBytes = MessageDigest.getInstance("SHA-512").digest(password.getBytes());
            StringBuilder str = new StringBuilder(2 * hashBytes.length);
            for (int i = 0; i < hashBytes.length; i++) {
                int v = hashBytes[i] & 0xff;
                str.append((char) HexChars[v >> 4]);
                str.append((char) HexChars[v & 0xf]);
            }
            return str.toString();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
	 * Saves new search frontend values or updates existing ones to database for the provided user and category
	 *
	 * @param user           user that saves the search
	 * @param category       category for which the search is being saved
	 * @param searchName     name of the search
	 * @param parametersList list of parameters containing their frontend values
	 * @return               new search id
	 */
    public static Integer saveSearch(Users user, Categories category, String searchName, ArrayList<Parameters> parametersList) {
        Searches search = new Searches(user, searchName);
        for (Searches savedSearch : user.getCategorySearches(category.getCatId())) if (savedSearch.getSrchSearchName().equals(searchName)) search = savedSearch;
        search.setSearchValues(parametersList);
        Integer newSearchId = SearchesDAO.updateSearchForCategory(search, category);
        if (newSearchId != null) {
            search.setSrchId(newSearchId);
            user.getCategorySearches(category.getCatId()).add(search);
        }
        return search.getSrchId();
    }

    /**
	 * Loads existing search frontend values from database
	 *
	 * @param user           user that loads the search
	 * @param category       category for which the search is being loaded
	 * @param searchId       id of the search that is being loaded
	 * @param parametersList list of parameters to set their fronend values
	 * @return               loaded search
	 */
    public static Searches loadSearch(Users user, Categories category, Integer searchId, ArrayList<Parameters> parametersList) {
        Searches search = null;
        for (Searches savedSearch : user.getCategorySearches(category.getCatId())) if (savedSearch.getSrchId() == searchId) search = savedSearch;
        for (Parameters parameter : parametersList) {
            parameter.setValue(search.getParameterValue(parameter.getId()));
            parameter.setPriority(search.getParameterPriority(parameter.getId()));
        }
        return search;
    }

    /**
	 * Loads existing search from database
	 *
	 * @param user           user that loads the search
	 * @param category       category for which the search is being deleted
	 * @param search         id of the search that is being deleted
	 * @param parametersList list of parameters to reset their fronend values
	 */
    public static void deleteSearch(Users user, Categories category, Searches search, ArrayList<Parameters> parametersList) {
        Iterator<Searches> searchesIterator = user.getCategorySearches(category.getCatId()).iterator();
        while (searchesIterator.hasNext()) if (searchesIterator.next() == search) searchesIterator.remove();
        SearchesDAO.deleteSearchById(search.getSrchId());
    }

    /**
	 * Computes collaborative ratings for the given user for all products
	 *
	 * @param user          user for whom collaborative ratings are being computed
	 * @param categoriesMap map of all categories to access all products
	 */
    public static void computeCollaborativeRatings(Users user, LinkedHashMap<String, ArrayList<Categories>> categoriesMap) {
        LinkedList<Users> otherUsers = UsersDAO.getOtherUsersByUser(user);
        if (otherUsers.size() == 0) return;
        for (ArrayList<Categories> categoryGroup : categoriesMap.values()) for (Categories category : categoryGroup) {
            for (Users otherUser : otherUsers) {
                Double categoryMatchRating = new Double(0);
                for (Products product : category.getProductsMap().values()) categoryMatchRating += Math.abs(user.getProductRating(product.getProdId()).getProdRtUserRating() - otherUser.getProductRating(product.getProdId()).getProdRtUserRating());
                otherUser.setCategoryMatchRating(1 - (categoryMatchRating / category.getProductsMap().size()));
            }
            Collections.sort(otherUsers, new UsersOrderingComparator());
            Double matchRatingsSum = new Double(0);
            LinkedList<Users> otherUsersStripped = new LinkedList<Users>();
            for (int i = 0; i < Math.min(otherUsers.size(), Constants.SIMILAR_USERS); i++) {
                otherUsersStripped.add(otherUsers.get(i));
                matchRatingsSum += otherUsers.get(i).getCategoryMatchRating();
            }
            for (Products product : category.getProductsMap().values()) {
                Double productCollaborativeRating = new Double(0);
                for (Users otherUser : otherUsersStripped) productCollaborativeRating += (otherUser.getCategoryMatchRating() / matchRatingsSum) * otherUser.getProductRating(product.getProdId()).getProdRtUserRating();
                user.getProductRating(product.getProdId()).setCollaborativeRating(productCollaborativeRating);
            }
        }
    }

    /**
	 * Updates product ratings for product to database
	 *
	 * @param productId id of the product
	 * @param user      user for whom the ratings are being updated
	 */
    public static void updateProductRatingForProductByUser(Integer productId, Users user) {
        ProductRatingsDAO.updateProductRatingForProductByUser(productId, user);
    }

    /**
	 * Updates product ratings for products to database
	 *
	 * @param products list of products
	 * @param user     user for whom the ratings are being updated
	 */
    public static void updateProductRatingForProductsByUser(LinkedList<Products> products, Users user) {
        for (Products product : products) ProductRatingsDAO.updateProductRatingForProductByUser(product.getProdId(), user);
    }
}
