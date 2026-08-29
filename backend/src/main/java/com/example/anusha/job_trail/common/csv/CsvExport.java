package com.example.anusha.job_trail.common.csv;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Streams a header row plus a list of already-flattened rows to an HTTP
 * response as a downloadable CSV — the one place every {@code /export}
 * endpoint's actual CSV writing lives. Quoting/escaping (a comma, a quote,
 * or a newline inside a note or reflection) is Commons CSV's job, not
 * ours — nothing here concatenates strings by hand.
 */
public final class CsvExport {

    private CsvExport() {
    }

    public static void write(HttpServletResponse response, String filename, List<String> header, List<List<String>> rows) {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (CSVPrinter printer = new CSVPrinter(response.getWriter(),
                CSVFormat.DEFAULT.builder().setHeader(header.toArray(String[]::new)).build())) {
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        } catch (IOException e) {
            // The response's own writer failing (a client that hung up
            // mid-download) isn't a recoverable app error — there's no
            // response left to report one on.
            throw new UncheckedIOException(e);
        }
    }
}
