package fast500;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by liuyufei on 1/08/17.
 */
public class Company implements Comparable<Company> {

    public final String name;
    public final String rank;
    public final String website;
    public final String country;
    public final String year;
    public final String growth;
    public final String field;

    private final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    private final String REF_SITE = "http://www.google.co.nz";
    private static final String EMAIL_FORMAT =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private final static Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_FORMAT);


    public Company(String rank, String name, String website, String country, String year, String growth, String field) {
        this.name = name;
        this.rank = rank;
        this.website = website.trim();
        this.country = country;
        this.year = year;
        this.growth = growth;
        this.field = field;
    }

    @Override
    public String toString() {
        return "Company{" +
                "name='" + name + '\'' +
                ", rank=" + rank +
                ", website='" + website + '\'' +
                ", country='" + country + '\'' +
                ", year='" + year + '\'' +
                ", growth='" + growth + '\'' +
                ", field='" + field + '\'' +
                "}\n";
    }

    @Override
    public int compareTo(Company o) {
        return Integer.parseInt(this.growth) - Integer.parseInt(o.growth);
    }

    private Optional<String> getContactURL() {
            return getDocumentFromURL("http://" + this.website)
                    .flatMap(doc-> doc.select("a").parallelStream()
                                    .filter(e -> "CONTACT US".equals(e.text().toUpperCase()) || "CONTACT".equals(e.text().toUpperCase())) //contact/contact us
                                    .filter(contact -> !contact.attr("href").contains("#")) //filter # out
                                    .map(contact -> {
                                        String tmpContact = contact.attr("href");
                                        if (!tmpContact.contains("http")) {
                                            return "http://" + this.website + tmpContact;
                                        }
                                        return tmpContact; //already has full url
                                    })
                                    .findFirst()
                    );

        }

    private Optional<Document> getDocumentFromURL(String url) {
        try {
            return Optional.of(Jsoup.connect(url)
                    .validateTLSCertificates(false) //ignore TSL validation,
                    .userAgent(USER_AGENT)
                    .referrer(REF_SITE) //simulate a real browser
                    .timeout(10000)
                    .get());
        } catch (IOException e) {
            System.out.println("ERROR: processing " + url + " has problem of " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getInfoEmailAddress() {
        Optional<String> contactLinkOpt = this.getContactURL();

        return contactLinkOpt
                .flatMap(contactLink -> getDocumentFromURL(contactLink)) //extract contact link
                .flatMap(doc-> doc.select("a").parallelStream() //extract contact document
                            .filter(a->EMAIL_PATTERN.matcher(a.text()).matches()) //look for email address
                            .filter(a->!a.text().contains("sale")&&!a.text().contains("support")&&!a.text().contains("account")) //don't want send my cv to sale or support team😅
                            .map(Element::text).findFirst() //may null
                );

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company)) return false;

        Company company = (Company) o;

        if (!website.equals(company.website)) return false;
        if (!country.equals(company.country)) return false;
        return field.equals(company.field);
    }

    @Override
    public int hashCode() {
        int result = website.hashCode();
        result = 31 * result + country.hashCode();
        result = 31 * result + field.hashCode();
        return result;
    }
}
