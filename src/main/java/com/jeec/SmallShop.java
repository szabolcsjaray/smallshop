package com.jeec;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.BoxLayout;

import java.awt.*;

public class SmallShop extends Frame implements TextListener, KeyListener {
    private static final String TEST_LOAD_LIST_POSITION = "loadtest!A1";
    private static final String LOAD_LIST_POSITION = "load!A1";
    private static final int MAX_INSTANT_LIST_SIZE = 20;
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String verzio = "1.1";

    /**
     * Global instance of the scopes required by this quickstart. If modifying these
     * scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private class Aru implements Comparable<Aru> {
        public String nev;
        public int ar;

        Aru(final String arunev, final int aruar) {
            nev = arunev;
            ar = aruar;
        }

        @Override
        public int compareTo( Aru other) {
            return huCollator.compare(nev, other.nev );
        }
    }

    private class LoadItem implements Comparable<LoadItem> {
        public Aru aru;
        public int num;

        LoadItem(final Aru a) {
            aru = a;
            num = 0;
        }

        @Override
        public int compareTo( LoadItem other) {
            return aru.compareTo( other.aru);
        }
    }

    private List<Aru> aruk = new ArrayList<>();
    private List<Aru> instantSearchList = new ArrayList<>();
    private ArrayList<LoadItem> loadList = new ArrayList<>();
    private TextField aruSearch;
    private java.awt.List selectListArea;
    private Panel leftPanel;
    private Panel leftBUttonPanel;
    private Panel rightPanel;
    private Button saveButton;
    private Button closeButton;
    private int selectN;
    int actualSelection = 0;
    private java.awt.List loadListArea;
    Sheets service;
    final String spreadsheetId = "1eELA6pYXF5PC1UIiNetW1jxnOBA05Tihe8iQymqLUbc";
    RuleBasedCollator huCollator = (RuleBasedCollator) Collator.getInstance(new Locale("hu", "HU"));

    SmallShop() {
        this.setTitle("SmallShop " + verzio);
        setSize(1000, 500);
        setLayout(new GridLayout(1,2));
        leftPanel = new Panel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setSize(200,500);
        leftPanel.setBackground(new Color(0x00, 0x9f, 0x9d));

        rightPanel = new Panel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setSize(400,500);
        add(leftPanel);
        add(rightPanel);

        aruSearch = new TextField("", 40);
        aruSearch.setMaximumSize(new Dimension(400, 50));
        aruSearch.addTextListener(this);
        aruSearch.addKeyListener(this);
        leftPanel.add(aruSearch);

        selectListArea = new java.awt.List(MAX_INSTANT_LIST_SIZE);
        selectListArea.setMaximumSize(new Dimension(1000, 400));
        selectListArea.setBackground(new Color(0x07, 0x45, 0x6f));
        selectListArea.setForeground(new Color(0xcd, 0xff, 0xeb));
        leftPanel.add(selectListArea);

        leftBUttonPanel = new Panel();
        leftBUttonPanel.setLayout(new BoxLayout(leftBUttonPanel, BoxLayout.X_AXIS));

        leftBUttonPanel.setMaximumSize(new Dimension(1000, 150));
        leftBUttonPanel.setBackground(new Color(0x00, 0x9f, 0x9d));
        leftBUttonPanel.setForeground(new Color(0xcd, 0xff, 0xeb));
        saveButton = new Button("Save");
        String fonts[] = Toolkit.getDefaultToolkit().getFontList();
        saveButton.setBackground(new Color(0x00, 0x9f, 0x9d));
        saveButton.setFont(new Font(fonts[0], Font.BOLD, 25));
        closeButton = new Button("Exit");
        closeButton.setFont(new Font(fonts[0], Font.BOLD, 25));
        closeButton.setBackground(new Color(0x00, 0x9f, 0x9d));
        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    saveData();
                } catch (Exception exc) {
                }
            }
        });
        closeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });


        leftBUttonPanel.add(saveButton);
        leftBUttonPanel.add(closeButton);

        leftPanel.add(leftBUttonPanel);

        loadListArea = new java.awt.List(MAX_INSTANT_LIST_SIZE);
        loadListArea.setForeground(new Color(0x07, 0x45, 0x6f));
        loadListArea.setBackground(new Color(0xcd, 0xff, 0xeb));
        rightPanel.add(loadListArea);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = SmallShop.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                        .setAccessType("offline").build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public void loadItems() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String range = "Arlista!C1:D";
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME).build();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            for (List row : values) {
                if (row.get(0) != null && !"***".equals(row.get(0)) && row.get(1) != null) {
                    try {
                        Aru a = new Aru((String) row.get(0), Integer.parseInt((String) row.get(1)));
                        aruk.add(a);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.print(".");
            }
        }
    }

    private void saveSumma(int summa) throws IOException, GeneralSecurityException {
        List<List<Object>> valuesAr = new ArrayList<>();
        List<Object> itemList = new ArrayList<Object>();
        valuesAr.add(itemList);
        itemList.add(summa);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.");
        String date = sdf.format(new Date());
        itemList.add(date);

        ValueRange body = new ValueRange().setValues(valuesAr);
        UpdateValuesResponse result = service.spreadsheets().values().update(spreadsheetId, "load!F1", body)
                .setValueInputOption("RAW").execute();
        System.out.printf("Save summa: %d cells updated.", result.getUpdatedCells());
    }

    private void clearData() throws IOException, GeneralSecurityException {
        List<List<Object>> valuesO = new ArrayList<>();
        int summa = 0;
        for (int i = 0;i<200;i++) {
            List<Object> itemList = new ArrayList<Object>();
            itemList.add("");
            itemList.add("");
            itemList.add("");
            itemList.add("");
            valuesO.add(itemList);
        }
        ValueRange body = new ValueRange().setValues(valuesO);
        UpdateValuesResponse result = service.spreadsheets().values().update(spreadsheetId, LOAD_LIST_POSITION, body)
                .setValueInputOption("RAW").execute();
        System.out.printf("Clear load data: %d cells updated.", result.getUpdatedCells());
    }


    private void saveData() throws IOException, GeneralSecurityException {
        clearData();
        List<List<Object>> valuesO = new ArrayList<>();
        int summa = 0;
        for (LoadItem item : loadList) {
            List<Object> itemList = new ArrayList<Object>();
            itemList.add(item.num);
            itemList.add(item.aru.nev);
            itemList.add(item.aru.ar);
            int itemAr = item.num*item.aru.ar;
            summa += itemAr;
            itemList.add(itemAr);
            valuesO.add(itemList);
        }
        ValueRange body = new ValueRange().setValues(valuesO);
        UpdateValuesResponse result = service.spreadsheets().values().update(spreadsheetId, LOAD_LIST_POSITION, body)
                .setValueInputOption("RAW").execute();
        System.out.printf("Save data: %d cells updated.", result.getUpdatedCells());
        saveSumma(summa);


    }

    public static void main(String[] args) {
        System.out.println("Start.");

        SmallShop me = new SmallShop();
        try {
            me.loadItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
        me.setVisible(true);
        me.updateSearchList();
    }

    private void updateSelectListArea() {
        String s = "";
        selectListArea.removeAll();
        int n = 0;
        for (Aru a : instantSearchList) {
            selectListArea.add(a.nev + " (" + a.ar + " Ft)");
            n++;
        }
        selectListArea.select(actualSelection);
    }

    private void addAru(final Aru a, int c) {
        if (!instantSearchList.contains(a)) {
            if (selectN >= MAX_INSTANT_LIST_SIZE) {
                return;
            }
            selectN++;
            instantSearchList.add(a);
        }
    }

    private LoadItem aruInLoadList() {
        for (LoadItem lItem:loadList) {
            if (lItem.aru.equals(instantSearchList.get(actualSelection))) {
                return lItem;
            }
        }
        return null;
    }

    private void addAruToLoad() {
        if (actualSelection<selectN) {
            LoadItem item = aruInLoadList();
            if (item==null) {
                LoadItem newItem = new LoadItem(instantSearchList.get(actualSelection));
                newItem.num++;
                loadList.add(newItem);
                System.out.println("added " + newItem.aru.nev);
            } else {
                item.num++;
            }
            updateLoadListArea();
        }
    }

    private void updateLoadListArea() {
        String areaString = "";
        int summaAr = 0;
        loadListArea.removeAll();
        Collections.sort(loadList);
        for(LoadItem item:loadList) {
            loadListArea.add("0000".substring(0, 4-Integer.toString(item.num, 10).length()) + item.num + "    " + item.aru.nev + " (" +
                    item.aru.ar + " Ft)              " + item.aru.ar*item.num + " Ft");
            summaAr += item.aru.ar*item.num;
        }
        loadListArea.add("Összesen: " + summaAr + " Ft" + areaString, 0);
        selectActual();
    }

    private void selectActual() {
        for (int i = 0;i<loadListArea.countItems();i++) {
            if (loadListArea.getItems()[i].indexOf(selectListArea.getItems()[actualSelection])!=-1) {
                loadListArea.select(i);
                loadListArea.makeVisible(i);
                break;
            }
        }
    }

    private void removeAruFromLoad() {
        LoadItem item = aruInLoadList();
        if (item!=null) {
            item.num--;
            if (item.num==0) {
                loadList.remove(item);
            }
            updateLoadListArea();
        }
    }

    @Override
    public void textValueChanged(TextEvent e) {
        if (e.getID() == TextEvent.TEXT_VALUE_CHANGED) {
            updateSearchList();
        }
    }

    private void updateSearchList() {
        String[] szavak = aruSearch.getText().toUpperCase().split(" ");
        instantSearchList = new ArrayList<>();
        selectN = 0;
        actualSelection = 0;

        for (Aru a : aruk) {
            String[] aruSzavak = a.nev.toUpperCase().split(" ");

            int count = szavak.length;
            int szoPos = 0;
            for (String szo : szavak) {
                int aruSzoPos = 0;
                for (String aruSzo : aruSzavak) {
                    if (szoPos == aruSzoPos && aruSzo.startsWith(szo)) {
                        count--;
                    }
                    aruSzoPos++;
                }
                szoPos++;
            }
            if (count == 0) {
                addAru(a, 1);
            }
        }

        for (Aru a : aruk) {
            String[] aruSzavak = a.nev.toUpperCase().split(" ");
            int count = szavak.length;
            for (String szo : szavak) {
                boolean found = false;
                for (String aruSzo : aruSzavak) {
                    if (!found && aruSzo.startsWith(szo)) {
                        count--;
                        found = true;
                    }
                }
            }
            if (count == 0) {
                addAru(a, 2);
            }
        }

        for (Aru a : aruk) {
            String[] aruSzavak = a.nev.toUpperCase().split(" ");
            int count = szavak.length;
            for (String szo : szavak) {
                boolean found = false;
                for (String aruSzo : aruSzavak) {
                    if (!found && aruSzo.contains(szo)) {
                        count--;
                        found = true;
                    }
                }
            }
            if (count == 0) {
                addAru(a, 3);
            }
        }

        updateSelectListArea();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ("Down".equals(KeyEvent.getKeyText(e.getKeyCode()))) {
            if (actualSelection < instantSearchList.size() - 1) {
                actualSelection++;
                updateSelectListArea();
            }
        } else if ("Up".equals(KeyEvent.getKeyText(e.getKeyCode()))) {
            if (actualSelection > 0) {
                actualSelection--;
                updateSelectListArea();
            }
        } else if ("Enter".equals(KeyEvent.getKeyText(e.getKeyCode()))) {
            if (e.isShiftDown()) {
                removeAruFromLoad();
            } else {
                addAruToLoad();
            }
        } else if ("Escape".equals(KeyEvent.getKeyText(e.getKeyCode()))) {
            aruSearch.setText("");
        }
    }


    @Override
    public void keyReleased(KeyEvent e) {
    }
}
