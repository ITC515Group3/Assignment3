package library;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import library.interfaces.EBorrowState;
import library.interfaces.IBorrowUI;
import library.interfaces.IBorrowUIListener;
import library.interfaces.daos.IBookDAO;
import library.interfaces.daos.ILoanDAO;
import library.interfaces.daos.IMemberDAO;
import library.interfaces.entities.EBookState;
import library.interfaces.entities.IBook;
import library.interfaces.entities.ILoan;
import library.interfaces.entities.IMember;
import library.interfaces.hardware.ICardReader;
import library.interfaces.hardware.ICardReaderListener;
import library.interfaces.hardware.IDisplay;
import library.interfaces.hardware.IPrinter;
import library.interfaces.hardware.IScanner;
import library.interfaces.hardware.IScannerListener;

public class BorrowUC_CTL implements ICardReaderListener,
        IScannerListener,
        IBorrowUIListener {

    private ICardReader reader;
    private IScanner scanner;
    private IPrinter printer;
    private IDisplay display;
    //private String state;
    private int scanCount = 0;
    private IBorrowUI ui;
    private EBorrowState state;
    private IBookDAO bookDAO;
    private IMemberDAO memberDAO;
    private ILoanDAO loanDAO;

    private List<IBook> bookList;
    private List<ILoan> loanList;
    private IMember borrower;

    private JPanel previous;


    public BorrowUC_CTL(ICardReader reader, IScanner scanner,
                        IPrinter printer, IDisplay display,
                        IBookDAO bookDAO, ILoanDAO loanDAO, IMemberDAO memberDAO) {

        this.display = display;
        this.ui = new BorrowUC_UI(this);
        state = EBorrowState.CREATED;
    }

    public void initialise() {
        previous = display.getDisplay();
        display.setDisplay((JPanel) ui, "Borrow UI");
    }

    public void close() {
        display.setDisplay(previous, "Main Menu");
    }

    @Override
    public void cardSwiped(int memberID) {
        System.out.println("cardSwiped: got " + memberID);
        if (!this.state.equals(EBorrowState.INITIALIZED)) {
            throw new RuntimeException(
                    String.format("BorrowUC_CTL : cardSwiped : illegal operation in state: %s", new Object[]{this.state}));
        }
        this.borrower = this.memberDAO.getMemberByID(memberID);
        if (this.borrower == null) {
            this.ui.displayErrorMessage(String.format("Member ID %d not found", new Object[]{Integer.valueOf(memberID)}));
            return;
        }
        boolean overdue = this.borrower.hasOverDueLoans();
        boolean atLoanLimit = this.borrower.hasReachedLoanLimit();
        boolean hasFines = this.borrower.hasFinesPayable();
        boolean overFineLimit = this.borrower.hasReachedFineLimit();
        boolean borrowing_restricted = (overdue) || (atLoanLimit) || (overFineLimit);
        if (borrowing_restricted) {
            setState(EBorrowState.BORROWING_RESTRICTED);
        } else {
            setState(EBorrowState.SCANNING_BOOKS);
        }
        int mID = this.borrower.getID();
        String mName = this.borrower.getFirstName() + " " + this.borrower.getLastName();
        String mContact = this.borrower.getContactPhone();
        this.ui.displayMemberDetails(mID, mName, mContact);
        if (hasFines) {
            float amountOwing = this.borrower.getFineAmount();
            this.ui.displayOutstandingFineMessage(amountOwing);
        }
        if (overdue) {
            this.ui.displayOverDueMessage();
        }
        if (atLoanLimit) {
            this.ui.displayAtLoanLimitMessage();
        }
        if (overFineLimit) {
            System.out.println("State: " + this.state);
            float amountOwing = this.borrower.getFineAmount();
            this.ui.displayOverFineLimitMessage(amountOwing);
        }
        String loanString = buildLoanListDisplay(this.borrower.getLoans());
        this.ui.displayExistingLoan(loanString);
    }


    @Override
    public void bookScanned(int barcode) {
        System.out.println("bookScanned: got " + barcode);
        if (this.state != EBorrowState.SCANNING_BOOKS) {
            throw new RuntimeException(
                    String.format("BorrowUC_CTL : bookScanned : illegal operation in state: %s", new Object[]{this.state}));
        }
        this.ui.displayErrorMessage("");

        IBook book = this.bookDAO.getBookByID(barcode);
        if (book == null) {
            this.ui.displayErrorMessage(String.format("Book %d not found", new Object[]{Integer.valueOf(barcode)}));
            return;
        }
        if (book.getState() != EBookState.AVAILABLE) {
            this.ui.displayErrorMessage(String.format("Book %d is not available: %s", new Object[]{Integer.valueOf(book.getID()), book.getState()}));
            return;
        }
        if (this.bookList.contains(book)) {
            this.ui.displayErrorMessage(String.format("Book %d already scanned: ", new Object[]{Integer.valueOf(book.getID())}));
            return;
        }
        this.scanCount += 1;
        this.bookList.add(book);
        ILoan loan = this.loanDAO.createLoan(this.borrower, book);
        this.loanList.add(loan);

        this.ui.displayScannedBookDetails(book.toString());

        this.ui.displayPendingLoan(buildLoanListDisplay(this.loanList));
        if (this.scanCount >= 5) {
            setState(EBorrowState.CONFIRMING_LOANS);
        }
    }


    private void setState(EBorrowState state) {
        System.out.println("Setting state: " + state);

        this.state = state;
        this.ui.setState(state);
        switch (state) {
            case CANCELLED:
                this.reader.setEnabled(true);
                this.scanner.setEnabled(false);
                break;
            case COMPLETED:
                this.reader.setEnabled(false);
                this.scanner.setEnabled(true);
                this.bookList = new ArrayList();
                this.loanList = new ArrayList();
                this.scanCount = this.borrower.getLoans().size();

                this.ui.displayScannedBookDetails("");

                this.ui.displayPendingLoan("");
                break;
            case CONFIRMING_LOANS:
                this.reader.setEnabled(false);
                this.scanner.setEnabled(false);

                this.ui.displayConfirmingLoan(buildLoanListDisplay(this.loanList));
                break;
            case CREATED:
                this.reader.setEnabled(false);
                this.scanner.setEnabled(false);
                for (ILoan loan : this.loanList) {
                    this.loanDAO.commitLoan(loan);
                }
                this.printer.print(buildLoanListDisplay(this.loanList));
                close();
                break;
            case SCANNING_BOOKS:
                this.reader.setEnabled(false);
                this.scanner.setEnabled(false);
                close();
                break;
            case INITIALIZED:
                this.reader.setEnabled(false);
                this.scanner.setEnabled(false);
                this.ui.displayErrorMessage(String.format("Member %d cannot borrow at this time.", new Object[]{Integer.valueOf(this.borrower.getID())}));
                break;
            default:
                throw new RuntimeException("Unknown state");
        }
    }

    @Override
    public void cancelled() {
        setState(EBorrowState.CANCELLED);
    }

    @Override
    public void scansCompleted() {
        setState(EBorrowState.CONFIRMING_LOANS);
    }

    @Override
    public void loansConfirmed() {
        setState(EBorrowState.COMPLETED);
    }

    @Override
    public void loansRejected() {

        System.out.println("Loans Rejected");
        setState(EBorrowState.SCANNING_BOOKS);
    }

    private String buildLoanListDisplay(List<ILoan> loans) {
        StringBuilder bld = new StringBuilder();
        for (ILoan ln : loans) {
            if (bld.length() > 0) bld.append("\n\n");
            bld.append(ln.toString());
        }
        return bld.toString();
    }

    public int getScanCount() {
        return scanCount;
    }

    public EBorrowState getState() {
        return state;
    }
}
