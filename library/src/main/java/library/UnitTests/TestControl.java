package library.UnitTests;

/**
 * Created by Tom on 20/9/16.
 */

import static junit.framework.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Calendar;
import java.util.Date;

import library.daos.BookHelper;
import library.daos.LoanHelper;
import library.daos.MemberHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import library.interfaces.entities.IBook;
import library.interfaces.entities.ILoan;
import library.interfaces.entities.IMember;
import library.interfaces.hardware.ICardReader;
import library.interfaces.hardware.IDisplay;
import library.interfaces.hardware.IPrinter;
import library.interfaces.hardware.IScanner;

import library.*;
import library.daos.*;
import library.interfaces.EBorrowState;
import library.interfaces.daos.*;


@RunWith(MockitoJUnitRunner.class)
public class TestControl {

    @Spy
    private IBookDAO bookDAO = new BookMapDAO(new BookHelper());
    @Spy
    private IMemberDAO memberDAO = new MemberMapDAO(new MemberHelper());
    @Spy
    private ILoanDAO loanDAO = new LoanMapDAO(new LoanHelper());

    @Mock
    private ICardReader reader;
    @Mock
    private IScanner scanner;
    @Mock
    private IPrinter printer;
    @Mock
    private IDisplay display;
    @Mock
    private BorrowUC_UI ui;

    @InjectMocks
    private BorrowUC_CTL sut;


    @Before
    public void setUp() throws Exception {
        setUpTestData();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testInit() {
        sut.initialise();

        verify(reader).setEnabled(true);
        verify(scanner).setEnabled(true);
//        verify(ui).setState(EBorrowState.INITIALIZED);
    }

    @Test
    public void testCardSwipedNoRestrictions() {
        //arrange
        sut.initialise();
        reset(reader);
        reset(scanner);
        reset(ui);

        //select a member with no restrictions, no existing loans
        int memberId = 1;

        //execute
        sut.cardSwiped(memberId);

        //asserts and verifies
        assertEquals(EBorrowState.SCANNING_BOOKS, sut.getState());
        verify(reader).setEnabled(false);
        verify(scanner).setEnabled(true);
        verify(ui).setState(EBorrowState.SCANNING_BOOKS);
        verify(ui).displayScannedBookDetails("");
        verify(ui).displayPendingLoan("");
        verify(ui).displayMemberDetails(eq(memberId), anyString(), anyString());
        verify(ui).displayExistingLoan(anyString());
        assertTrue(sut.getScanCount() == 0);
    }

    private void setUpTestData() {
        IBook[] book = new IBook[15];
        IMember[] member = new IMember[6];

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

        member[0] = memberDAO.addMember("fName0", "lName0", "0001", "email0");
        member[1] = memberDAO.addMember("fName1", "lName1", "0002", "email1");
        member[2] = memberDAO.addMember("fName2", "lName2", "0003", "email2");
        member[3] = memberDAO.addMember("fName3", "lName3", "0004", "email3");
        member[4] = memberDAO.addMember("fName4", "lName4", "0005", "email4");
        member[5] = memberDAO.addMember("fName5", "lName5", "0006", "email5");

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();

        //create a member with overdue loans
        for (int i = 0; i < 2; i++) {
            ILoan loan = loanDAO.createLoan(member[1], book[i]);
            loanDAO.commitLoan(loan);
        }
        cal.setTime(now);
        cal.add(Calendar.DATE, ILoan.LOAN_PERIOD + 1);
        Date checkDate = cal.getTime();
        loanDAO.updateOverDueStatus(checkDate);

        //create a member with maxed out unpaid fines
        member[2].addFine(10.0f);

        //create a member with maxed out loans
        for (int i = 2; i < 7; i++) {
            ILoan loan = loanDAO.createLoan(member[3], book[i]);
            loanDAO.commitLoan(loan);
        }

        //a member with a fine, but not over the limit
        member[4].addFine(5.0f);

        //a member with a couple of loans but not over the limit
        for (int i = 7; i < 9; i++) {
            ILoan loan = loanDAO.createLoan(member[5], book[i]);
            loanDAO.commitLoan(loan);
        }
    }

}