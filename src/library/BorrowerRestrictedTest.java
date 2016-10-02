package library.tests.integration;

import static org.junit.Assert.*;
import org.junit.*;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import library.daos.*;
import library.hardware.CardReader;
import library.interfaces.*;
import library.interfaces.daos.IBookDAO;
import library.interfaces.daos.ILoanDAO;
import library.interfaces.daos.IMemberDAO;
import library.interfaces.entities.*;
import library.interfaces.hardware.*;
import library.BorrowUC_CTL;
import library.BorrowUC_UI;

import java.util.Calendar;
import java.util.Date;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class BorrowerRestrictedTest 
{
	//1.1.1
	private BorrowUC_CTL controlClass;
	private IBookDAO bookMapDao;
	private ILoanDAO loanMapDao;
	private IMemberDAO memberMapDao;
	
	//7
	@Mock
	private ICardReader cardReader;
	
	//8
	@Mock
	private IScanner scanner;
	@Mock
	private IPrinter printer;
	@Mock
	private IDisplay display;
	//2
	@Mock
	private BorrowUC_UI ui;
	  
	Date borrowDate;
	Date returnDate;
	Calendar calender_;
	
	@Before
	public void setUp() throws Exception
	{	//9
		cardReader = mock(CardReader.class);
		scanner = mock(IScanner.class);
		printer = mock(IPrinter.class);
		display = mock(IDisplay.class);
		ui = mock(BorrowUC_UI.class);

		bookMapDao = new BookMapDAO(new BookHelper());
		memberMapDao = new MemberMapDAO(new MemberHelper());
		loanMapDao = new LoanMapDAO(new LoanHelper());

		controlClass = new BorrowUC_CTL(cardReader, scanner, printer, display, bookMapDao, loanMapDao, memberMapDao,ui);

		IBook[] book = new IBook[15];
		IMember[] member = new IMember[6];

		book[0]  = bookMapDao.addBook("author1", "title1", "callNumber1");
		book[1]  = bookMapDao.addBook("author1", "title2", "callNumber2");
		book[2]  = bookMapDao.addBook("author1", "title3", "callNumber3");
		book[3]  = bookMapDao.addBook("author1", "title4", "callNumber4");
		book[4]  = bookMapDao.addBook("author2", "title5", "callNumber5");
		book[5]  = bookMapDao.addBook("author2", "title6", "callNumber6");
		book[6]  = bookMapDao.addBook("author2", "title7", "callNumber7");
		book[7]  = bookMapDao.addBook("author2", "title8", "callNumber8");
		book[8]  = bookMapDao.addBook("author3", "title9", "callNumber9");
		book[9]  = bookMapDao.addBook("author3", "title10", "callNumber10");
		book[10] = bookMapDao.addBook("author4", "title11", "callNumber11");
		book[11] = bookMapDao.addBook("author4", "title12", "callNumber12");
		book[12] = bookMapDao.addBook("author5", "title13", "callNumber13");
		book[13] = bookMapDao.addBook("author5", "title14", "callNumber14");
		book[14] = bookMapDao.addBook("author5", "title15", "callNumber15");

		member[0] = memberMapDao.addMember("firstName0", "lastName0", "0001", "email0");
		member[1] = memberMapDao.addMember("firstName1", "lastName1", "0002", "email1");
		member[2] = memberMapDao.addMember("firstName2", "lastName2", "0003", "email2");
		member[3] = memberMapDao.addMember("firstName3", "lastName3", "0004", "email3");
		member[4] = memberMapDao.addMember("firstName4", "lastName4", "0005", "email4");
		member[5] = memberMapDao.addMember("firstName5", "lastName5", "0006", "email5");

		calender_ = Calendar.getInstance();
		Date now = calender_.getTime();
		
		for(int i = 0; i < 2; i++)
		{
			ILoan testLoan = loanMapDao.createLoan(member[1], book[i]);
			loanMapDao.commitLoan(testLoan);
		}
		
	    calender_.setTime(now);
	    calender_.add(Calendar.DATE, ILoan.LOAN_PERIOD + 1);
	    Date checkDate = calender_.getTime();     
	    loanMapDao.updateOverDueStatus(checkDate);
	    
	    member[2].addFine(10.0f);
	    
	    for(int i = 2; i < 7; i++)
	    {
	    	ILoan testLoan = loanMapDao.createLoan(member[3], book[i]);
	    	loanMapDao.commitLoan(testLoan);
	    }
	}


	@After
	public void reset() throws Exception
	{
	    controlClass = null;
	    bookMapDao = null;
	    loanMapDao = null;
	    memberMapDao = null;
	    cardReader = null;
	    scanner = null;
	    printer = null;
	    display = null;
	    ui = null;
	}
	
	//9
	@Test
	public void testCardSwiped()
	{		
		controlClass.setState(EBorrowState.INITIALIZED);
		controlClass.cardSwiped(1);
		
		verify(cardReader).setEnabled(false);
		verify(scanner).setEnabled(true);
		verify(ui).setState(EBorrowState.SCANNING_BOOKS);
		verify(ui).displayMemberDetails(1, "firstName0 lastName0", "0001");
		verify(ui).displayExistingLoan(any(String.class));    

		assertEquals(EBorrowState.SCANNING_BOOKS, controlClass.getState());
	}

	//9.1.12 to 9.1.12.1
	@Test
	public void testBorrowRestrictedWithFines()
	{
		//9.1
		controlClass.setState(EBorrowState.INITIALIZED);
		controlClass.cardSwiped(3);

		verify(cardReader).setEnabled(false);
		//9.1.1
		verify(ui).setState(EBorrowState.BORROWING_RESTRICTED);
		verify(ui).displayMemberDetails(3, "firstName2 lastName2", "0003");
		//9.1.12
		verify(ui).displayOutstandingFineMessage(10.0f);
		//9.1.14
		verify(ui).displayOverFineLimitMessage(10.0f);  
		//9.1.9
		assertEquals(EBorrowState.BORROWING_RESTRICTED, controlClass.getState());
	}

	//9.1.14 to 9.1.14.1
	@Test
	public void testBorrowRestrictedWithOverLimit()
	{
		//9.1
		controlClass.setState(EBorrowState.INITIALIZED);
		controlClass.cardSwiped(4);
		
		verify(cardReader).setEnabled(false);
		//9.1.1
		verify(ui).setState(EBorrowState.BORROWING_RESTRICTED);
		verify(ui).displayMemberDetails(4, "firstName3 lastName3", "0004");
		//9.1.14
		verify(ui).displayAtLoanLimitMessage();
		//9.1.15
		verify(ui).displayExistingLoan(any(String.class));
		//9.1.9
		assertEquals(EBorrowState.BORROWING_RESTRICTED, controlClass.getState());
	}

	//9.1.13 to 9.1.13.1 
	@Test
	public void testBorrowRestrictedOverDueLoan()
	{
		//9.1
		controlClass.setState(EBorrowState.INITIALIZED);
		controlClass.cardSwiped(2);

		verify(cardReader).setEnabled(false);
		//9.1.1
		verify(ui).setState(EBorrowState.BORROWING_RESTRICTED);
		//verify(ui).displayMemberDetails(2, "firstName0 lastName0", "0002");
		//9.1.11
		verify(ui).displayExistingLoan(any(String.class));
		//9.1.11.1
		verify(ui).displayOverDueMessage();
		//9.1.9
		assertEquals(EBorrowState.BORROWING_RESTRICTED, controlClass.getState());
	}

	//9.1.1
	@Test
	public void testBorrowMemberDoesNotExist()
	{
		//9.1
		controlClass.setState(EBorrowState.INITIALIZED);
		controlClass.cardSwiped(7);

		verify(cardReader).setEnabled(true);
		//9.1.1
		verify(scanner).setEnabled(false);
		verify(ui).setState(EBorrowState.INITIALIZED);
		verify(ui).displayErrorMessage(any(String.class));

		assertEquals(EBorrowState.INITIALIZED, controlClass.getState());
	}	
}
