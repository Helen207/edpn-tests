package sk.edpn.spec;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/** NCZI codelist excerpts used by the tests, keyed by codelist OID. */
public final class Codelists {

    public record Codelist(String name, String version, Map<String, String> codes) {
    }

    private record File(String source, Map<String, Codelist> codelists) {
    }

    private final Map<String, Codelist> byOid;

    public Codelists() {
        try (InputStream in = Codelists.class.getClassLoader().getResourceAsStream("codelists.json")) {
            if (in == null) {
                throw new IllegalStateException("codelists.json nie je na classpath");
            }
            this.byOid = new ObjectMapper().readValue(in, File.class).codelists();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Codelist get(String oid) {
        Codelist codelist = byOid.get(oid);
        if (codelist == null) {
            throw new IllegalArgumentException("Číselník " + oid + " chýba v codelists.json");
        }
        return codelist;
    }
}
