package ch.smartlink.smartticketdemo.control;

import java.math.BigDecimal;
import java.util.Arrays;

import org.osptalliance.cipurse.CipurseException;
import org.osptalliance.cipurse.ICommsChannel;
import org.osptalliance.cipurse.ILogger;
import org.osptalliance.cipurse.commands.*;
import org.osptalliance.cipurse.commands.CommandAPI.Version;

import ch.smartlink.smartticketdemo.Account;
import ch.smartlink.smartticketdemo.util.Constant;
import ch.smartlink.smartticketdemo.util.MessageUtil;

public class PaymentCardCreator {


    private static final int COLD_RESET = 0;
    private ICommsChannel commsChannel;
    private ILogger logger;
    private CipurseCardHandler cipurseCardHandler;
    private ICipurseOperational cipurseOperational;
    private ICipurseAdministration cipurseAdministration;

    public PaymentCardCreator(ICommsChannel commsChannel, ILogger logger) {
        this.commsChannel = commsChannel;
        this.logger = logger;

    }

    private void initCommand() throws CipurseException {
        cipurseCardHandler = new CipurseCardHandler(commsChannel, null, logger);
        CommandAPI cmdApi = CommandAPIFactory.getInstance().buildCommandAPI();
        cmdApi.setVersion(Version.V2);
        cipurseOperational = cmdApi.getCipurseOperational(cipurseCardHandler);
        cipurseAdministration = cmdApi.getCipurseAdministration(cipurseCardHandler);
        cipurseCardHandler.open();
    }

    public void installApplication() throws CipurseException {
        initCommand();
        formatAll();
        selectMF();
        createSmartlinkTicketADF();
        selectSmartlinkTicketADF();
        createFileCardAccount();
        createFileCardTransaction();

    }

    private void formatAll() throws CipurseException {
        // 00 A4 00 00 02 3F 00 00
        String selectCommandForFormat = "00 A4 00 00 02 3F 00 00";
        ByteArray response = cipurseCardHandler.transmit(new ByteArray(selectCommandForFormat));
        System.out.println("Select file format: " + response);
        String format = "80 FC 00 00 07 43 6F 6E 66 69 72 4D";
        response = cipurseCardHandler.transmit(new ByteArray(format));
        System.out.println("Select file format: " + response);

    }

    private void createFileCardTransaction() throws CipurseException {
        String command = "00 E0 00 00 09 92 01 06 06 00 30 02 0A 31";
        ByteArray response = cipurseCardHandler.transmit(new ByteArray(command));


        System.out.println("Create File Card Transaction: " + response);
        clearCardTransaction();
        response = cipurseCardHandler.transmit(new ByteArray("80 7E 00 00"));
        System.out.println("Commit Tranaction: " + response);
    }

    private void createFileCardAccount() throws CipurseException {
        //                00 E0 00 00 09 92 01 06 01 00 30 01 00 28
        //String command = "00 E0 00 00 09 92 01 06 01 00 30 01 00 28";
        // ByteArray response= cipurseCardHandler.transmit(new ByteArray(command));

        EFFileAttributes efFileAttributes = new EFFileAttributes();
        efFileAttributes.fileID = Constant.ID_FILE_USER_DATA;
        efFileAttributes.fileType = EFFileAttributes.BINARY_FILE_TYPE;
        efFileAttributes.numOfRecs = 1;
        efFileAttributes.fileSize = Constant.LENGH_USER_DATA_BIN;
        efFileAttributes.SFID = 0x01;
        ByteArray response = cipurseAdministration.createFileEF(efFileAttributes);
        System.out.println("Create Card Account Response: " + response);

        // select file
        //00 A4 00 00 02 30 01
        //   response= cipurseCardHandler.transmit(new ByteArray("00 A4 00 00 02 30 01"));

        System.out.println("Create Card Account Response: " + response);
        //00 A4 00 00 02 30 01
        // update binary
        //response= cipurseCardHandler.transmit(new ByteArray("00 D6 00 00 28 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));
        response = cipurseOperational.updateBinary((short) 0, new ByteArray("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));
        System.out.println("Create empty record: " + response);
        //00 D6 00 00 28 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        // perform tranaction
        //80 7E 00 00
        // response= cipurseCardHandler.transmit(new ByteArray("80 7E 00 00"));
        response = cipurseOperational.performTransaction();
        System.out.println("Commit Tranaction: " + response);
    }

    private void createSmartlinkTicketADF() throws CipurseException {
//		String commandCreateADF = "00 E0 00 00 1B";
        //String commandCreateADF = "00 E0 00 00 10 92 00 0D 38 40 30 00 08 05 00 62 04 84 02 D0 01";

//		//      00 E0 00 00 1B 92 00 18 38 40 30 00 08 05 00 62 0F 84 0D D2 76 00 00 04 15 02 00 00 03 00 01 01
//		String adfAttribute = "92 00 18 38 40 30 00 08 05 00 62 0F 84 0D ";
//		String adfId = Constant.ID_ADF_SMARTLINK_TICKET;
        //      92 00 18 38 40 30 00 08 05 00 62 0F 84 0D
        //      92 00 0D 38 40 30 00 08 05 00 62 04 84 02 D0 01
        DFFileAttributes smartlinkTicketDFAtribute = new DFFileAttributes();
        smartlinkTicketDFAtribute.appProfile = DFFileAttributes.PROFILE_T;
        smartlinkTicketDFAtribute.fileID = 0x3000;
        smartlinkTicketDFAtribute.numOfEFs = 8;
        smartlinkTicketDFAtribute.numOfSFIDs = 5;
        smartlinkTicketDFAtribute.fileDescriptor = 0x38;

        smartlinkTicketDFAtribute.setAIDValue(new ByteArray(Constant.ID_ADF_SMARTLINK_TICKET));
        //smartlinkTicketDFAtribute.
        ByteArray response = cipurseAdministration.createFileADF(smartlinkTicketDFAtribute);
//		ByteArray response= cipurseCardHandler.transmit(new ByteArray(commandCreateADF + adfAttribute + adfId));
//
        System.out.println("Create ADF Response: " + response);
    }

    public void initCardInfo(Account account) throws CipurseException {

        selectMF();
        selectSmartlinkTicketADF();
        selectFileCardAccount();
        storeCardInfo(account);

    }

    private void clearCardTransaction() throws CipurseException {
        //String command = "00 E2 00 00 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
        String command = "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
        for (int i = 0; i < 10; i++) {
            // ByteArray response=	cipurseCardHandler.transmit(new ByteArray(command));B
            ByteArray response = cipurseOperational.appendRecord(new ByteArray(command));
            System.out.println("Append record: " + i + " : " + response);
        }
    }

    private void storeCardInfo(Account account) throws CipurseException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MessageUtil.leftZeroPadding(account.getCardNumber(), 16)).append(" ");
        stringBuilder.append(account.getExpiryDate()).append(" ");
        stringBuilder.append(MessageUtil.formatBalanceToStore(account.getBalance())).append(" ");
        stringBuilder.append(account.getCurrency());
        ByteArray cardData = new ByteArray(stringBuilder.toString().getBytes());
        cipurseOperational.updateBinary((short) 0, cardData);


    }

    private void selectFileCardAccount() throws CipurseException {
        // SELECT User Account file
        ByteArray response = cipurseOperational.selectFilebyFID(Constant.ID_FILE_USER_DATA);
        System.out.println("Select User Account Response : " + response);


    }

    private void selectSmartlinkTicketADF() throws CipurseException {
        //String command = "00 A4 04 00 0D D2 76 00 00 04 15 02 00 00 03 00 01 01 00";
        //	response = cipurseOperational.selectFilebyAID(new ByteArray(Constant.ID_ADF_SMARTLINK_TICKET));
        ByteArray response = cipurseOperational.selectFilebyAID(new ByteArray(Constant.ID_ADF_SMARTLINK_TICKET));
        System.out.println("Select ADF Smartlink Ticket Response : " + response);


    }

    private void selectMF() throws CipurseException {


        System.out.println("------------ Smartlink Verify Access ------------");

        ByteArray baAtr = cipurseCardHandler.reset(COLD_RESET);
        System.out.println("ATQ after default reset received : " + baAtr);

        // Select MF
        ByteArray response = cipurseOperational.selectMF();
        System.out.println("Select MF Response : " + response);
    }

    private boolean isSmartlinkTicketADFNotExist() throws CipurseException {
        ByteArray response = cipurseOperational.selectFilebyAID(new ByteArray(Constant.ID_ADF_SMARTLINK_TICKET));
        System.out.println("Select ADF Smartlink Ticket Response : " + response);
        return isFileNotExist(response);
    }

    private boolean isFileNotExist(ByteArray response) {
        return new ByteArray("6A 82").equals(response);
    }

    private boolean isResponseSuccess(ByteArray response) {
        return new ByteArray("09 00").equals(response);
    }


    private void isUserRecordExist() {

    }
}
