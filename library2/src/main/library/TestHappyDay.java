package library;

/**
 * Created by Tom on 21/9/16.
 */

import library.daos.*;
import library.interfaces.EBorrowState;
import library.interfaces.daos.IBookDAO;
import library.interfaces.daos.ILoanDAO;
import library.interfaces.daos.IMemberDAO;
import library.interfaces.entities.IBook;
import library.interfaces.entities.ILoan;
import library.interfaces.entities.IMember;
import library.interfaces.hardware.ICardReader;
import library.interfaces.hardware.IDisplay;
import library.interfaces.hardware.IPrinter;
import library.interfaces.hardware.IScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TestHappyDay {

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
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInit() {
        sut.initialise();

        verify(reader).setEnabled(true);
        verify(scanner).setEnabled(false);

        verify(ui).setState(EBorrowState.INITIALIZED);
        System.out.println("Case: System Initialized\n");
    }

    @Test
    public void testHappyDayScenarioBorrowSuccessful() {
        //1
        sut.initialise();

        int borrowerID = 1;
        int bookBarcode = 10;

        //9.1
        sut.cardSwiped(borrowerID);
        //9.1.7
        verify(reader).setEnabled(true);
        //9.1.8
        verify(scanner).setEnabled(false);
        //9.1.9
        verify(ui).setState(EBorrowState.SCANNING_BOOKS);
        //9.1.10
        verify(ui).displayScannedBookDetails("");
        //9.1.11
        verify(ui).displayPendingLoan("");
        //9.1.12
        verify(ui).displayMemberDetails(eq(borrowerID), anyString(), anyString());
        //9.1.15
        verify(ui).displayExistingLoan("");

        //10 scan a book
        sut.bookScanned(bookBarcode);

        verifyScanCount(1);

        // 11.1
        sut.scansCompleted();

        //11.1.1
        verify(ui).setState(EBorrowState.CONFIRMING_LOANS);
        //11.1.2
        verify(reader, times(2)).setEnabled(false);
        //11.1.3
        verify(scanner, times(2)).setEnabled(false);
        //11.1.5
        verify(ui).displayConfirmingLoan(anyString());

        //12.1
        sut.loansConfirmed();
        //12.1.1
        verify(ui).setState(EBorrowState.COMPLETED);
        //12.1.4
        verify(printer).print(anyString());
        //12.1.5
        verify(scanner, times(3)).setEnabled(false);
        //12.1.6
        verify(reader, times(3)).setEnabled(false);

        //12.1.7
        try {
            Class<?> clazz = sut.getClass();
            Field field = clazz.getDeclaredField("previous");
            field.setAccessible(true);
            JPanel previous = (JPanel) field.get(sut);

            verify(this.display).setDisplay(previous, "Main Menu");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        System.out.println("Case: Book Borrowed Successful\n");
    }

    @Test
    public void testHappyDayScenarioBorrowBookNotFound() {
        //arrange
        sut.initialise();

        int borrowerID = 1;
        int bookBarcode = 100;
        sut.cardSwiped(borrowerID);

        verify(reader).setEnabled(true);
        verify(scanner).setEnabled(false);

        verify(ui).setState(EBorrowState.SCANNING_BOOKS);

        verify(ui).displayScannedBookDetails("");
        verify(ui).displayPendingLoan("");
        verify(ui).displayMemberDetails(eq(borrowerID), anyString(), anyString());
        verify(ui).displayExistingLoan("");

        // scan a book
        sut.bookScanned(bookBarcode);

        verifyScanCount(0);

        // 11.1
        sut.scansCompleted();

        verify(ui).setState(EBorrowState.CONFIRMING_LOANS);
        verify(reader).setEnabled(true);
        verify(scanner, times(2)).setEnabled(false);

        verify(ui).displayErrorMessage("");
        verify(ui).displayErrorMessage(String.format("Book %d not found", bookBarcode));

        System.out.println("Case: Book Not found\n");
    }

    @Test
    public void testHappyDayScenarioBorrowBookNotAvailable() {
        //arrange
        sut.initialise();

        int borrowerID = 1;
        int bookBarcode = 1;
        sut.cardSwiped(borrowerID);

        verify(reader).setEnabled(true);
        verify(scanner).setEnabled(false);

        verify(ui).setState(EBorrowState.SCANNING_BOOKS);

        verify(ui).displayScannedBookDetails("");
        verify(ui).displayPendingLoan("");
        verify(ui).displayMemberDetails(eq(borrowerID), anyString(), anyString());
        verify(ui).displayExistingLoan("");

        // scan a book
        sut.bookScanned(bookBarcode);

        verifyScanCount(0);

        // 11.1
        sut.scansCompleted();

        verify(ui).setState(EBorrowState.CONFIRMING_LOANS);
        verify(reader).setEnabled(true);
        verify(scanner, times(2)).setEnabled(false);

        verify(ui).displayErrorMessage("");
        verify(ui).displayErrorMessage(String.format("Book %d is not available: %s", bookBarcode, bookDAO.getBookByID(bookBarcode).getState()));
        System.out.println("Case: Book is Not Available\n");
    }

    @Test
    public void testHappyDayScenarioBorrowBookAlreadyScanned() {
        //1 initialise program
        sut.initialise();

        int borrowerID = 1;
        int bookBarcode = 10;
        sut.cardSwiped(borrowerID);

        verify(reader).setEnabled(true);
        verify(scanner).setEnabled(false);

        verify(ui).setState(EBorrowState.SCANNING_BOOKS);

        verify(ui).displayScannedBookDetails("");
        verify(ui).displayPendingLoan("");
        verify(ui).displayMemberDetails(eq(borrowerID), anyString(), anyString());
        verify(ui).displayExistingLoan("");

        //10 scan a book
        sut.bookScanned(bookBarcode);

        verifyScanCount(1);

        //10 scan again
        sut.bookScanned(bookBarcode);

        verify(ui, times(2)).displayErrorMessage("");
        verify(ui).displayErrorMessage(String.format("Book %d already scanned: ", bookBarcode));
    }

    private void verifyScanCount(int iCount) {
        try {
            Field field = sut.getClass().getDeclaredField("scanCount");
            field.setAccessible(true);
            int count = (int) field.get(sut);
            assertTrue(count == iCount);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
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
