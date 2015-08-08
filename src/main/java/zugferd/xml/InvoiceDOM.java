/*
 * $Id$
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 2014-2015 iText Group NV
 * Authors: Bruno Lowagie, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package zugferd.xml;

import zugferd.exceptions.DataIncompleteException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import zugferd.codes.CountryCode;
import zugferd.codes.CurrencyCode;
import zugferd.codes.DateFormatCode;
import zugferd.codes.DocumentTypeCode;
import zugferd.codes.FreeTextSubjectCode;
import zugferd.codes.GlobalIdentifierCode;
import zugferd.codes.NumberChecker;
import zugferd.codes.PaymentMeansCode;
import zugferd.codes.TaxCategoryCode;
import zugferd.codes.TaxIDTypeCode;
import zugferd.codes.TaxTypeCode;
import zugferd.exceptions.InvalidCodeException;

/**
 * @author iText
 */
public final class InvoiceDOM {
    
    // Profiles that are supported:
    public static final String TEMPLATE = "resources/zugferd/zugferd-template.xml";
    
    // The DOM document
    private Document doc;
    
    /**
     * Creates an object that will import data into an XML template.
     * @param data If this is an instance of BASICInvoice, the BASIC profile will be used;
     *             If this is an instance of COMFORTInvoice, the COMFORT profile will be used.
     */
    public InvoiceDOM(BASICInvoice data)
            throws ParserConfigurationException, SAXException, IOException,
            DataIncompleteException, InvalidCodeException {
        // Loading the XML template
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	doc = docBuilder.parse(TEMPLATE);
        // importing the data
        importSpecifiedExchangedDocumentContext(data);
        importHeaderExchangedDocument(data);
        importSpecifiedSupplyChainTradeTransaction(data);
    }
    
    /**
     * Checks if a string is empty and throws a DataIncompleteException if so.
     * @param   s   the String to check
     * @param   message the message if an exception is thrown   
     */
    protected void check(String s, String message) throws DataIncompleteException {
        if (s == null || s.trim().length() == 0)
            throw new DataIncompleteException(message);
    }
    
    /**
     * Imports the data for the following tag: rsm:SpecifiedExchangedDocumentContext
     * @param   data    the invoice data
     */
    private void importSpecifiedExchangedDocumentContext(BASICInvoice data) {
        Element element = (Element) doc.getElementsByTagName("rsm:SpecifiedExchangedDocumentContext").item(0);
        // TestIndicator (optional)
        setContent(element, "udt:Indicator", data.getTestIndicator() ? "true" : "false", null);
    }
    
    /**
     * Helper method to set the content of a tag.
     * @param parent    the parent element of the tag
     * @param tag       the tag for which we want to set the content
     * @param content   the new content for the tag
     * @param attributes    a sequence of attributes of which
     *                      the odd elements are keys, the even elements the
     *                      corresponding value.
     */
    private void setContent(Element parent, String tag, String content, String[] attributes) {
        Node node = parent.getElementsByTagName(tag).item(0);
        // content
        node.setTextContent(content);
        // attributes
        if (attributes == null || attributes.length == 0)
            return;
        int n = attributes.length;
        String attrName, attrValue;
        NamedNodeMap attrs = node.getAttributes();
        Node attr;
        for (int i = 0; i < n; i++) {
            attrName = attributes[i];
            if (++i == n) continue;
            attrValue = attributes[i];
            attr = attrs.getNamedItem(attrName);
            if (attr != null)
                attr.setTextContent(attrValue);
        }
    }
    
    /**
     * Imports the data for the following tag: rsm:HeaderExchangedDocument
     * @param   data    the invoice data
     */
    private void importHeaderExchangedDocument(BASICInvoice data)
            throws DataIncompleteException, InvalidCodeException {
        Element element = (Element) doc.getElementsByTagName("rsm:HeaderExchangedDocument").item(0);
        // ID (required)
        check(data.getId(), "HeaderExchangedDocument > ID");
        setContent(element, "ram:ID", data.getId(), null);
        // Name (required)
        check(data.getName(), "HeaderExchangedDocument > Name");
        setContent(element, "ram:Name", data.getName(), null);
        // TypeCode (required)
        DocumentTypeCode dtCode = new DocumentTypeCode(
                data instanceof COMFORTInvoice ? DocumentTypeCode.COMFORT : DocumentTypeCode.BASIC);
        setContent(element, "ram:TypeCode", dtCode.check(data.getTypeCode()), null);
        // IssueDateTime (required)
        check(data.getDateTimeFormat(), "HeaderExchangedDocument > DateTimeString");
        setDateTime(element, "udt:DateTimeString", data.getDateTimeFormat(), data.getDateTime());
        // IncludedNote (optional): header level
        setIncludedNotes(element, FreeTextSubjectCode.HEADER, data);
    }
    
    /**
     * Set the content of a date tag along with the attribute that defines the format.
     * @param parent    the parent element that holds the date tag
     * @param tag       the date tag we want to change
     * @param dateTimeFormat    the format that will be used as an attribute
     * @param dateTime  the actual date
     */
    protected void setDateTime(Element parent, String tag, String dateTimeFormat, Date dateTime)
            throws InvalidCodeException {
        if (dateTimeFormat == null) return;
        DateFormatCode dfCode = new DateFormatCode();
        setContent(parent, tag, dfCode.convertToString(dateTime, dfCode.check(dateTimeFormat)), new String[]{"format", dateTimeFormat});
    }
    
    /**
     * Includes notes and (in case of the COMFORT profile) the subject codes
     * for those notes.
     * @param parent    the parent element of the tag we want to change
     * @param level the level where the notices are added (header or line)
     * @param data  the invoice data (BASICInvoice and COMFORTInvoice are supported)
     */
    protected void setIncludedNotes(Element parent, int level, BASICInvoice data)
        throws DataIncompleteException, InvalidCodeException {
        Node includedNoteNode = parent.getElementsByTagName("ram:IncludedNote").item(0);
        String[] notes = data.getNotes();
        int n = notes.length;
        FreeTextSubjectCode ftsCode = new FreeTextSubjectCode(level);
        String[] notesCodes = null;
        if (data instanceof COMFORTInvoice) {
            notesCodes = ((COMFORTInvoice)data).getNotesCodes();
            if (n != notesCodes.length)
                throw new DataIncompleteException("Number of included notes is not equal to number of codes for included notes.");
        }
        for (int i = 0; i < n; i++) {
            Element noteNode = (Element)includedNoteNode.cloneNode(true);
            Node content = noteNode.getElementsByTagName("ram:Content").item(0);
            content.setTextContent(notes[i]);
            if (notesCodes != null) {
                Node code = noteNode.getElementsByTagName("ram:SubjectCode").item(0);
                code.setTextContent(ftsCode.check(notesCodes[i]));
            }
            parent.insertBefore(noteNode, includedNoteNode);
        }
    }
    /**
     * Imports the data for the following tag: rsm:SpecifiedSupplyChainTradeTransaction
     * @param   data    the invoice data
     */
    private void importSpecifiedSupplyChainTradeTransaction(BASICInvoice data) throws DataIncompleteException, InvalidCodeException {
        Element element = (Element) doc.getElementsByTagName("rsm:SpecifiedSupplyChainTradeTransaction").item(0);
        
        /* ram:ApplicableSupplyChainTradeAgreement */

        // buyer reference (optional; comfort only)
        setBuyerReference(element, data);
        // SellerTradeParty (required)
        check(data.getSellerName(), "SpecifiedSupplyChainTradeTransaction > ApplicableSupplyChainTradeAgreement > SellerTradeParty > Name");
        setSellerTradeParty(element, data);
        // BuyerTradeParty (required)
        check(data.getBuyerName(), "SpecifiedSupplyChainTradeTransaction > ApplicableSupplyChainTradeAgreement > BuyerTradeParty > Name");
        setBuyerTradeParty(element, data);
        
        /* ram:ApplicableSupplyChainTradeDelivery */
        
        if (data instanceof COMFORTInvoice) {
            COMFORTInvoice cData = (COMFORTInvoice)data;
            // BuyerOrderReferencedDocument (optional)
            Element document = (Element)element.getElementsByTagName("ram:BuyerOrderReferencedDocument").item(0);
            setDateTime(document, "ram:IssueDateTime", cData.getBuyerOrderReferencedDocumentIssueDateTimeFormat(), cData.getBuyerOrderReferencedDocumentIssueDateTime());
            setContent(document, "ram:ID", cData.getBuyerOrderReferencedDocumentID(), null);
            // ContractReferencedDocument (optional)
            document = (Element)element.getElementsByTagName("ram:ContractReferencedDocument").item(0);
            setDateTime(document, "ram:IssueDateTime", cData.getContractReferencedDocumentIssueDateTimeFormat(), cData.getContractReferencedDocumentIssueDateTime());
            setContent(document, "ram:ID", cData.getContractReferencedDocumentID(), null);
            // CustomerOrderReferencedDocument (optional)
            document = (Element)element.getElementsByTagName("ram:CustomerOrderReferencedDocument").item(0);
            setDateTime(document, "ram:IssueDateTime", cData.getCustomerOrderReferencedDocumentIssueDateTimeFormat(), cData.getCustomerOrderReferencedDocumentIssueDateTime());
            setContent(document, "ram:ID", cData.getCustomerOrderReferencedDocumentID(), null);
        }
        
        /* ram:ApplicableSupplyChainTradeDelivery */
        // ActualDeliverySupplyChainEvent (optional)
        Element parent = (Element)element.getElementsByTagName("ram:ActualDeliverySupplyChainEvent").item(0);
        setDateTime(parent, "udt:DateTimeString", data.getDeliveryDateTimeFormat(), data.getDeliveryDateTime());
        // DeliveryNoteReferencedDocument (optional)
        if (data instanceof COMFORTInvoice) {
            COMFORTInvoice cData = (COMFORTInvoice)data;
            Element document = (Element)element.getElementsByTagName("ram:DeliveryNoteReferencedDocument").item(0);
            setDateTime(document, "ram:IssueDateTime", cData.getDeliveryNoteReferencedDocumentIssueDateTimeFormat(), cData.getDeliveryNoteReferencedDocumentIssueDateTime());
            setContent(document, "ram:ID", cData.getDeliveryNoteReferencedDocumentID(), null);
        }
        
        /* ram:ApplicableSupplyChainTradeSettlement */
        // ram:PaymentReference (optional)
        setContent(element, "ram:PaymentReference", data.getPaymentReference(), null);
        // ram:InvoiceCurrencyCode (required)
        CurrencyCode currCode = new CurrencyCode();
        setContent(element, "ram:InvoiceCurrencyCode", currCode.check(data.getInvoiceCurrencyCode()), null);
        // ram:InvoiceeTradeParty (optional)
        if (data instanceof COMFORTInvoice) {
            setInvoiceeTradeParty(element, (COMFORTInvoice)data);
        }
        
        // ram:SpecifiedTradeSettlementPaymentMeans
        parent = (Element)element.getElementsByTagName("ram:ApplicableSupplyChainTradeSettlement").item(0);
        processPaymentMeans(parent, data);
        
        // ram:ApplicableTradeTax
        processTax(parent, data);
        
        if (data instanceof COMFORTInvoice) {
            
            // ram:BillingSpecifiedPeriod
            
            COMFORTInvoice cData = (COMFORTInvoice)data;
            Element period = (Element)element.getElementsByTagName("ram:BillingSpecifiedPeriod").item(0);
            Element start = (Element)period.getElementsByTagName("ram:StartDateTime").item(0);
            setDateTime(start, "udt:DateTimeString", cData.getBillingStartDateTimeFormat(), cData.getBillingStartDateTime());
            // ContractReferencedDocument (optional)
            Element end = (Element)period.getElementsByTagName("ram:EndDateTime").item(0);
            setDateTime(end, "udt:DateTimeString", cData.getBillingEndDateTimeFormat(), cData.getBillingEndDateTime());
            
            // ram:SpecifiedTradeAllowanceCharge
            processSpecifiedTradeAllowanceCharge(parent, cData);
        }
        
        check(data.getLineTotalAmount(), "SpecifiedTradeSettlementMonetarySummation > LineTotalAmount");
        check(data.getLineTotalAmountCurrencyID(), "SpecifiedTradeSettlementMonetarySummation > LineTotalAmount . currencyID");
        setNodeContent(doc, "ram:LineTotalAmount", 0, data.getLineTotalAmount(), "currencyID", data.getLineTotalAmountCurrencyID());
        check(data.getChargeTotalAmount(), "SpecifiedTradeSettlementMonetarySummation > ChargeTotalAmount");
        check(data.getChargeTotalAmountCurrencyID(), "SpecifiedTradeSettlementMonetarySummation > ChargeTotalAmount . currencyID");
        setNodeContent(doc, "ram:ChargeTotalAmount", 0, data.getChargeTotalAmount(), "currencyID", data.getChargeTotalAmountCurrencyID());
        check(data.getAllowanceTotalAmount(), "SpecifiedTradeSettlementMonetarySummation > AllowanceTotalAmount");
        check(data.getAllowanceTotalAmountCurrencyID(), "SpecifiedTradeSettlementMonetarySummation > AllowanceTotalAmount . currencyID");
        setNodeContent(doc, "ram:AllowanceTotalAmount", 0, data.getAllowanceTotalAmount(), "currencyID", data.getAllowanceTotalAmountCurrencyID());
        check(data.getTaxBasisTotalAmount(), "SpecifiedTradeSettlementMonetarySummation > TaxBasisTotalAmount");
        check(data.getTaxBasisTotalAmountCurrencyID(), "SpecifiedTradeSettlementMonetarySummation > TaxBasisTotalAmount . currencyID");
        setNodeContent(doc, "ram:TaxBasisTotalAmount", 0, data.getTaxBasisTotalAmount(), "currencyID", data.getTaxBasisTotalAmountCurrencyID());
        check(data.getTaxTotalAmount(), "SpecifiedTradeSettlementMonetarySummation > TaxTotalAmount");
        check(data.getTaxTotalAmountCurrencyID(), "SpecifiedTradeSettlementMonetarySummation > TaxTotalAmount . currencyID");
        setNodeContent(doc, "ram:TaxTotalAmount", 0, data.getTaxTotalAmount(), "currencyID", data.getTaxTotalAmountCurrencyID());
        check(data.getGrandTotalAmount(), "SpecifiedTradeSettlementMonetarySummation > GrandTotalAmount");
        check(data.getGrandTotalAmountCurrencyID(), "SpecifiedTradeSettlementMonetarySummation > GrandTotalAmount . currencyID");
        setNodeContent(doc, "ram:GrandTotalAmount", 0, data.getGrandTotalAmount(), "currencyID", data.getGrandTotalAmountCurrencyID());
        processLines(doc, data);
    }
    
    protected void setBuyerReference(Element parent, BASICInvoice data) {
        if (data instanceof COMFORTInvoice) {
            String buyerReference = ((COMFORTInvoice) data).getBuyerReference();
            setContent(parent, "ram:BuyerReference", buyerReference, null);
        }
    }
    
    protected void setSellerTradeParty(Element parent, BASICInvoice data) throws DataIncompleteException, InvalidCodeException {
        String id = null;
        String[] globalID = null;
        String[] globalIDScheme = null;
        if (data instanceof COMFORTInvoice) {
            id = ((COMFORTInvoice)data).getSellerID();
            globalID = ((COMFORTInvoice)data).getSellerGlobalID();
            globalIDScheme = ((COMFORTInvoice)data).getSellerGlobalSchemeID();
        }
        String name = data.getSellerName();
        String postcode = data.getSellerPostcode();
        String lineOne = data.getSellerLineOne();
        String lineTwo = data.getSellerLineTwo();
        String cityName = data.getSellerCityName();
        String countryID = data.getSellerCountryID();
        String[] taxRegistrationID = data.getSellerTaxRegistrationID();
        String[] taxRegistrationSchemeID = data.getSellerTaxRegistrationSchemeID();
        processTradeParty(parent, "ram:SellerTradeParty", id, globalID, globalIDScheme,
                name, postcode, lineOne, lineTwo, cityName, countryID,
                taxRegistrationID, taxRegistrationSchemeID);
    }
    
    protected void setBuyerTradeParty(Element parent, BASICInvoice data) throws DataIncompleteException, InvalidCodeException {
        String id = null;
        String[] globalID = null;
        String[] globalIDScheme = null;
        if (data instanceof COMFORTInvoice) {
            id = ((COMFORTInvoice)data).getBuyerID();
            globalID = ((COMFORTInvoice)data).getBuyerGlobalID();
            globalIDScheme = ((COMFORTInvoice)data).getBuyerGlobalSchemeID();
        }
        String name = data.getBuyerName();
        String postcode = data.getBuyerPostcode();
        String lineOne = data.getBuyerLineOne();
        String lineTwo = data.getBuyerLineTwo();
        String cityName = data.getBuyerCityName();
        String countryID = data.getBuyerCountryID();
        String[] taxRegistrationID = data.getBuyerTaxRegistrationID();
        String[] taxRegistrationSchemeID = data.getBuyerTaxRegistrationSchemeID();
        processTradeParty(parent, "ram:BuyerTradeParty", id, globalID, globalIDScheme,
                name, postcode, lineOne, lineTwo, cityName, countryID,
                taxRegistrationID, taxRegistrationSchemeID);
    }
    
    protected void setInvoiceeTradeParty(Element parent, COMFORTInvoice data) throws DataIncompleteException, InvalidCodeException {
        String name = data.getInvoiceeName();
        if (name == null) return;
        String id = data.getInvoiceeID();
        String[] globalID = data.getInvoiceeGlobalID();
        String[] globalIDScheme = data.getInvoiceeGlobalSchemeID();
        String postcode = data.getInvoiceePostcode();
        String lineOne = data.getInvoiceeLineOne();
        String lineTwo = data.getInvoiceeLineTwo();
        String cityName = data.getInvoiceeCityName();
        String countryID = data.getInvoiceeCountryID();
        String[] taxRegistrationID = data.getInvoiceeTaxRegistrationID();
        String[] taxRegistrationSchemeID = data.getInvoiceeTaxRegistrationSchemeID();
        processTradeParty(parent, "ram:InvoiceeTradeParty", id, globalID, globalIDScheme,
                name, postcode, lineOne, lineTwo, cityName, countryID,
                taxRegistrationID, taxRegistrationSchemeID);
    }
    
    protected void processTradeParty(Element element, String tagname,
        String id, String[] globalID, String[] globalIDScheme,
        String name, String postcode, String lineOne, String lineTwo,
        String cityName, String countryID,
        String[] taxRegistrationID, String[] taxRegistrationSchemeID) throws DataIncompleteException, InvalidCodeException {
        Element party = (Element) element.getElementsByTagName(tagname).item(0);
        Node node;
        if (id != null) {
            node = party.getElementsByTagName("ram:ID").item(0);
            node.setTextContent(id);
        }
        if (globalID != null) {
            GlobalIdentifierCode giCode = new GlobalIdentifierCode();
            int n = globalID.length;
            if (globalIDScheme == null || globalIDScheme.length != n)
                throw new DataIncompleteException("Number of global ID schemes is not equal to number of global IDs.");
            node = party.getElementsByTagName("ram:GlobalID").item(0);
            for (int i = 0; i < n; i++) {
                Element idNode = (Element)node.cloneNode(true);
                NamedNodeMap attrs = idNode.getAttributes();
                idNode.setTextContent(globalID[i]);
                Node schemeID = attrs.getNamedItem("schemeID");
                schemeID.setTextContent(giCode.check(globalIDScheme[i]));
                party.insertBefore(idNode, node);
            }
        }
        setContent(party, "ram:Name", name, null);
        setContent(party, "ram:PostcodeCode", postcode, null);
        setContent(party, "ram:LineOne", lineOne, null);
        setContent(party, "ram:LineTwo", lineTwo, null);
        setContent(party, "ram:CityName", cityName, null);
        if (countryID != null) {
            CountryCode cCode = new CountryCode();
            setContent(party, "ram:CountryID", cCode.check(countryID), null);
        }
        int n = taxRegistrationID.length;
        if (taxRegistrationSchemeID != null && taxRegistrationSchemeID.length != n)
            throw new DataIncompleteException("Number of tax ID schemes is not equal to number of tax IDs.");
        Element tax = (Element) party.getElementsByTagName("ram:SpecifiedTaxRegistration").item(0);
        node = tax.getElementsByTagName("ram:ID").item(0);
        TaxIDTypeCode tCode = new TaxIDTypeCode();
        for (int i = 0; i < n; i++) {
            Element idNode = (Element)node.cloneNode(true);
            idNode.setTextContent(taxRegistrationID[i]);
            NamedNodeMap attrs = idNode.getAttributes();
            Node schemeID = attrs.getNamedItem("schemeID");
            schemeID.setTextContent(tCode.check(taxRegistrationSchemeID[i]));
            tax.insertBefore(idNode, node);
        }           
    }
    
    protected void processPaymentMeans(Element parent, BASICInvoice data) throws InvalidCodeException {
        String[] pmID = data.getPaymentMeansID();
        int n = pmID.length;
        String[] pmTypeCode = new String[n];
        String[][] pmInformation = new String[n][];
        String[] pmSchemeAgencyID = data.getPaymentMeansSchemeAgencyID();
        String[] pmPayerIBAN = new String[n];
        String[] pmPayerProprietaryID = new String[n];
        String[] pmIBAN = data.getPaymentMeansPayeeAccountIBAN();
        String[] pmAccountName = data.getPaymentMeansPayeeAccountAccountName();
        String[] pmAccountID = data.getPaymentMeansPayeeAccountProprietaryID();
        String[] pmPayerBIC = new String[n];
        String[] pmPayerGermanBankleitzahlID = new String[n];
        String[] pmPayerFinancialInst = new String[n];
        String[] pmBIC = data.getPaymentMeansPayeeFinancialInstitutionBIC();
        String[] pmGermanBankleitzahlID = data.getPaymentMeansPayeeFinancialInstitutionGermanBankleitzahlID();
        String[] pmFinancialInst = data.getPaymentMeansPayeeFinancialInstitutionName();
        if (data instanceof COMFORTInvoice) {
            COMFORTInvoice cData = (COMFORTInvoice)data;
            pmTypeCode = cData.getPaymentMeansTypeCode();
            pmInformation = cData.getPaymentMeansInformation();
            pmPayerIBAN = cData.getPaymentMeansPayerAccountIBAN();
            pmPayerProprietaryID = cData.getPaymentMeansPayerAccountProprietaryID();
            pmPayerBIC = cData.getPaymentMeansPayerFinancialInstitutionBIC();
            pmPayerGermanBankleitzahlID = cData.getPaymentMeansPayerFinancialInstitutionGermanBankleitzahlID();
            pmPayerFinancialInst = cData.getPaymentMeansPayerFinancialInstitutionName();
        }
        Node node = parent.getElementsByTagName("ram:SpecifiedTradeSettlementPaymentMeans").item(0);
        for (int i = 0; i < pmID.length; i++) {
            Node newNode = node.cloneNode(true);
            processPaymentMeans((Element)newNode, data,
                    pmTypeCode[i],
                    pmInformation[i],
                    pmID[i],
                    pmSchemeAgencyID[i],
                    pmPayerIBAN[i],
                    pmPayerProprietaryID[i],
                    pmIBAN[i],
                    pmAccountName[i],
                    pmAccountID[i],
                    pmPayerBIC[i],
                    pmPayerGermanBankleitzahlID[i],
                    pmPayerFinancialInst[i],
                    pmBIC[i],
                    pmGermanBankleitzahlID[i],
                    pmFinancialInst[i]
            );
            parent.insertBefore(newNode, node);
        }
    }
    
    protected void processPaymentMeans(Element parent, BASICInvoice data,
        String typeCode, String[] information,
        String id, String scheme,
        String payerIban, String payerProprietaryID,
        String iban, String accName, String accID,
        String payerBic, String payerBank, String payerInst,
        String bic, String bank, String inst) throws InvalidCodeException {
        if (typeCode != null) {
            setContent(parent, "ram:TypeCode", new PaymentMeansCode().check(typeCode), null);
        }
        if (information != null) {
            Node node = parent.getElementsByTagName("ram:Information").item(0);
            for (String info : information) {
                Node newNode = node.cloneNode(true);
                newNode.setTextContent(info);
                parent.insertBefore(newNode, node);
            }
        }
        setContent(parent, "ram:ID", id, new String[]{"schemeAgencyID", scheme});
        Element payer = (Element)parent.getElementsByTagName("ram:PayerPartyDebtorFinancialAccount").item(0);
        setContent(payer, "ram:IBANID", payerIban, null);
        setContent(payer, "ram:ProprietaryID", payerProprietaryID, null);
        Element payee = (Element)parent.getElementsByTagName("ram:PayeePartyCreditorFinancialAccount").item(0);
        setContent(payee, "ram:IBANID", iban, null);
        setContent(payee, "ram:AccountName", accName, null);
        setContent(payee, "ram:ProprietaryID", accID, null);
        payer = (Element)parent.getElementsByTagName("ram:PayerSpecifiedDebtorFinancialInstitution").item(0);
        setContent(payer, "ram:BICID", payerBic, null);
        setContent(payer, "ram:GermanBankleitzahlID", payerBank, null);
        setContent(payer, "ram:Name", payerInst, null);
        payee = (Element)parent.getElementsByTagName("ram:PayeeSpecifiedCreditorFinancialInstitution").item(0);
        setContent(payee, "ram:BICID", bic, null);
        setContent(payee, "ram:GermanBankleitzahlID", bank, null);
        setContent(payee, "ram:Name", inst, null);
    }
    
    protected void processTax(Element parent, BASICInvoice data) throws InvalidCodeException, DataIncompleteException {
        String[] calculated = data.getTaxCalculatedAmount();
        int n = calculated.length;
        String[] calculatedCurr = data.getTaxCalculatedAmountCurrencyID();
        String[] typeCode = data.getTaxTypeCode();
        String[] exemptionReason = new String[n];
        String[] basisAmount = data.getTaxBasisAmount();
        String[] basisAmountCurr = data.getTaxBasisAmountCurrencyID();
        String[] category = new String[n];
        String[] percent = data.getTaxApplicablePercent();
        if (data instanceof COMFORTInvoice) {
            COMFORTInvoice cData = (COMFORTInvoice)data;
            exemptionReason = cData.getTaxExemptionReason();
            category = cData.getTaxCategoryCode();
        }
        Node node = parent.getElementsByTagName("ram:ApplicableTradeTax").item(0);
        for (int i = 0; i < n; i++) {
            Node newNode = node.cloneNode(true);
            processTax((Element)newNode, calculated[i], calculatedCurr[i], typeCode[i],
                exemptionReason[i], basisAmount[i], basisAmountCurr[i],
                category[i], percent[i]);
            parent.insertBefore(newNode, node);
        }
    }
    
    protected void processTax(Element parent,
        String calculatedAmount, String currencyID, String typeCode,
        String exemptionReason, String basisAmount, String basisAmountCurr,
        String category, String percent)
            throws InvalidCodeException, DataIncompleteException {
        // Calculated amount (required; 2 decimals)
        NumberChecker dec2 = new NumberChecker(NumberChecker.TWO_DECIMALS);
        CurrencyCode currCode = new CurrencyCode();
        check(currCode.check(currencyID), "ApplicableTradeTax > CalculatedAmount > CurrencyID");
        setContent(parent, "ram:CalculatedAmount", dec2.check(calculatedAmount), new String[]{"currencyID", currencyID});
        // TypeCode (required)
        check(typeCode, "ApplicableTradeTax > TypeCode");
        setContent(parent, "ram:TypeCode", new TaxTypeCode().check(typeCode), null);
        // exemption reason (optional)
        setContent(parent, "ram:ExemptionReason", exemptionReason, null);
        // basis amount (required, 2 decimals)
        check(currCode.check(basisAmountCurr), "ApplicableTradeTax > BasisAmount > CurrencyID");
        setContent(parent, "ram:BasisAmount", dec2.check(basisAmount), new String[]{"currencyID", basisAmountCurr});
        // Category code (optional)
        if (category != null) {
            setContent(parent, "ram:CategoryCode", new TaxCategoryCode().check(category), null);
        }
        // Applicable percent (required; 2 decimals)
        setContent(parent, "ram:ApplicablePercent", dec2.check(percent), null);
    }
    
    private void processSpecifiedTradeAllowanceCharge(Element parent, COMFORTInvoice data) throws InvalidCodeException {
        String[] indicator = data.getTradeAllowanceChargeIndicator();
        String[] actualAmount = data.getTradeAllowanceChargeActualAmount();
        String[] actualAmountCurr = data.getTradeAllowanceChargeActualAmountCurrency();
        String[] reason = data.getTradeAllowanceChargeReason();
        String[][] typeCode = data.getTradeAllowanceChargeTaxTypeCode();
        String[][] categoryCode = data.getTradeAllowanceChargeTaxCategoryCode();
        String[][] percentage = data.getTradeAllowanceChargeTaxApplicablePercent();
        Node node = (Element)parent.getElementsByTagName("ram:SpecifiedTradeAllowanceCharge").item(0);
        for (int i = 0; i < indicator.length; i++) {
            Node newNode = node.cloneNode(true);
            processSpecifiedTradeAllowanceCharge((Element)newNode, indicator[i],
                actualAmount[i], actualAmountCurr[i], reason[i],
                typeCode[i], categoryCode[i], percentage[i]);
            parent.insertBefore(newNode, node);
        }
    }
    
    public void processSpecifiedTradeAllowanceCharge(Element parent, String indicator,
        String actualAmount, String actualAmountCurrency, String reason,
        String[] typeCode, String[] categoryCode, String[] percent) throws InvalidCodeException {
        setContent(parent, "udt:Indicator", indicator, null);
        NumberChecker dec4 = new NumberChecker(NumberChecker.FOUR_DECIMALS);
        CurrencyCode currCode = new CurrencyCode();
        setContent(parent, "ram:ActualAmount", dec4.check(actualAmount), new String[]{"currencyID", currCode.check(actualAmountCurrency)});
        setContent(parent, "ram:Reason", reason, null);
        Node node = parent.getElementsByTagName("ram:CategoryTradeTax").item(0);
        TaxTypeCode tCode = new TaxTypeCode();
        TaxCategoryCode cCode = new TaxCategoryCode();
        NumberChecker dec2 = new NumberChecker(NumberChecker.TWO_DECIMALS);
        for (int i = 0; i < typeCode.length; i++) {
            Element newNode = (Element) node.cloneNode(true);
            setContent(newNode, "ram:TypeCode", tCode.check(typeCode[i]), null);
            setContent(newNode, "ram:CategoryCode", cCode.check(categoryCode[i]), null);
            setContent(newNode, "ram:ApplicablePercent", dec2.check(percent[i]), null);
            parent.insertBefore(newNode, node);
        }
    }
    
    protected void processLines(Document doc, BASICInvoice data) throws DataIncompleteException {
        String[] quantity = data.getLineItemBilledQuantity();
        if (quantity.length == 0)
            throw new DataIncompleteException("You can create an invoice without any line items");
        String[] quantityCode = data.getLineItemBilledQuantityUnitCode();
        String[] name = data.getLineItemSpecifiedTradeProductName();
        for (int i = quantity.length - 1; i >= 0; i--) {
            processLine(doc, quantity[i], quantityCode[i], name[i]);
        }
    }
    
    protected void processLine(Document doc, String quantity, String code, String name) {
        Node node = doc.getElementsByTagName("ram:IncludedSupplyChainTradeLineItem").item(0);
        Node newNode = node.cloneNode(true);
        Node childNode;
        NodeList list = newNode.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            childNode = list.item(i);
            if ("ram:SpecifiedSupplyChainTradeDelivery".equals(childNode.getNodeName())) {
                NodeList l = childNode.getChildNodes();
                for (int j = 0; j < l.getLength(); j++) {
                    Node grandchildNode = l.item(j);
                    if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
                        grandchildNode.setTextContent(quantity);
                        grandchildNode.getAttributes().item(0).setTextContent(code);
                    }
                }
            }
            else if ("ram:SpecifiedTradeProduct".equals(childNode.getNodeName())) {
                NodeList l = childNode.getChildNodes();
                for (int j = 0; j < l.getLength(); j++) {
                    Node grandchildNode = l.item(j);
                    if (grandchildNode.getNodeType() == Node.ELEMENT_NODE)
                        grandchildNode.setTextContent(name);
                }
            }
        }
        Node parent = node.getParentNode();
        parent.insertBefore(newNode, node);
    }
    
    
    protected void setNodeContent(Document doc, String tagname, int idx, String content, String... attributes) {
        Node node = doc.getElementsByTagName(tagname).item(idx);
        if (node == null)
            return;
        node.setTextContent(content);
        int n = attributes.length;
        if (n == 0) return;
        String attrName, attrValue;
        NamedNodeMap attrs = node.getAttributes();
        Node attr;
        for (int i = 0; i < n; i++) {
            attrName = attributes[i];
            if (++i == n) continue;
            attrValue = attributes[i];
            attr = attrs.getNamedItem(attrName);
            if (attr != null)
                attr.setTextContent(attrValue);
        }
    }
    
    public byte[] exportDoc() throws TransformerException {
        removeNodes(doc);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	DOMSource source = new DOMSource(doc);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult(out);
        transformer.transform(source, result);
        return out.toByteArray();
    }
    
    protected static void removeNodes(Node node) {
        NodeList list = node.getChildNodes();
        for (int i = list.getLength() - 1; i >= 0; i--) {
            removeNodes(list.item(i));
        }
        boolean emptyElement = node.getNodeType() == Node.ELEMENT_NODE
            && node.getChildNodes().getLength() == 0;
        boolean emptyText = node.getNodeType() == Node.TEXT_NODE
            && node.getNodeValue().trim().isEmpty();
        if (emptyElement || emptyText) {
            node.getParentNode().removeChild(node);
        }
    }
}
