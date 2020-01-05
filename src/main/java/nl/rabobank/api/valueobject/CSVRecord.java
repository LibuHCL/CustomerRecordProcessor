package nl.rabobank.api.valueobject;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CSVRecord {
  private Integer reference;
  private String accountNumber;
  private String description;
  private BigDecimal startBalance;
  private BigDecimal mutation;
  private BigDecimal endBalance;

  public Integer validateReferenceAttribute() {
    return reference;
  }
}
