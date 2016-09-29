package library.UnitTests;

import org.junit.Test;
import static org.junit.Assert.*;

import library.interfaces.daos.IBookDAO;
import library.interfaces.entities.IBook;

/**
 * Created by Tom on 20/9/16.
 */
public class TestBookMapDAO {
    private IBookDAO bookDAO;
    @Test

    private void setupTestData() {
        IBook[] book = new IBook[15];

        book[0] = bookDAO.addBook("author1", "title1", "callNo1");
        book[1] = bookDAO.addBook("author1", "title2", "callNo2");
        book[2] = bookDAO.addBook("author1", "title3", "callNo3");
        book[3] = bookDAO.addBook("author1", "title4", "callNo4");
        book[4] = bookDAO.addBook("author2", "title5", "callNo5");
        book[5] = bookDAO.addBook("author2", "title6", "callNo6");
        book[6] = bookDAO.addBook("author2", "title7", "callNo7");
        book[7] = bookDAO.addBook("author2", "title8", "callNo8");
        book[8] = bookDAO.addBook("author3", "title9", "callNo9");
        book[9] = bookDAO.addBook("author3", "title10", "callNo10");
        book[10] = bookDAO.addBook("author4", "title11", "callNo11");
        book[11] = bookDAO.addBook("author4", "title12", "callNo12");
        book[12] = bookDAO.addBook("author5", "title13", "callNo13");
        book[13] = bookDAO.addBook("author5", "title14", "callNo14");
        book[14] = bookDAO.addBook("author5", "title15", "callNo15");
    }
}