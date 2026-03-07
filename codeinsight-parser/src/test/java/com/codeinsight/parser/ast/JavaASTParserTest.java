package com.codeinsight.parser.ast;

import com.codeinsight.model.code.ParsedClass;
import com.codeinsight.model.code.ParsedMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaASTParserTest {

    private JavaASTParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaASTParser();
    }

    @Test
    void shouldParseSimpleClass() {
        String source = """
                package com.example;

                import java.util.List;

                public class UserService {
                    private final UserRepository repository;

                    public UserService(UserRepository repository) {
                        this.repository = repository;
                    }

                    public List<User> findAll() {
                        return repository.findAll();
                    }
                }
                """;

        Optional<ParsedClass> result = parser.parse("UserService.java", source);

        assertThat(result).isPresent();
        ParsedClass parsed = result.get();
        assertThat(parsed.getClassName()).isEqualTo("UserService");
        assertThat(parsed.getPackageName()).isEqualTo("com.example");
        assertThat(parsed.getQualifiedName()).isEqualTo("com.example.UserService");
        assertThat(parsed.getClassType()).isEqualTo("CLASS");
        assertThat(parsed.getFields()).hasSize(1);
        assertThat(parsed.getFields().getFirst().getName()).isEqualTo("repository");
        assertThat(parsed.getMethods()).hasSize(1); // findAll (constructors excluded)
        assertThat(parsed.getImports()).contains("java.util.List");
    }

    @Test
    void shouldParseAnnotations() {
        String source = """
                package com.example;

                @Service
                @Transactional
                public class OrderService {
                    @Autowired
                    private OrderRepo repo;

                    @GetMapping("/orders")
                    public void listOrders() {}
                }
                """;

        Optional<ParsedClass> result = parser.parse("OrderService.java", source);

        assertThat(result).isPresent();
        ParsedClass parsed = result.get();
        assertThat(parsed.getAnnotations()).containsExactly("Service", "Transactional");
        assertThat(parsed.getFields().getFirst().getAnnotations()).contains("Autowired");
        assertThat(parsed.getMethods().getFirst().getAnnotations()).contains("GetMapping");
    }

    @Test
    void shouldCalculateCyclomaticComplexity() {
        String source = """
                package com.example;

                public class ComplexClass {
                    public int calculate(int x) {
                        if (x > 0) {
                            for (int i = 0; i < x; i++) {
                                if (i % 2 == 0) {
                                    return i;
                                }
                            }
                        } else {
                            while (x < 0) {
                                x++;
                            }
                        }
                        return x;
                    }
                }
                """;

        Optional<ParsedClass> result = parser.parse("ComplexClass.java", source);

        assertThat(result).isPresent();
        ParsedMethod method = result.get().getMethods().getFirst();
        assertThat(method.getName()).isEqualTo("calculate");
        assertThat(method.getComplexity()).isGreaterThanOrEqualTo(4); // 1 + if + for + if + while
    }

    @Test
    void shouldParseInterface() {
        String source = """
                package com.example;

                public interface Repository<T> {
                    T findById(String id);
                    void save(T entity);
                }
                """;

        Optional<ParsedClass> result = parser.parse("Repository.java", source);

        assertThat(result).isPresent();
        ParsedClass parsed = result.get();
        assertThat(parsed.getClassType()).isEqualTo("INTERFACE");
        assertThat(parsed.getMethods()).hasSize(2);
    }

    @Test
    void shouldParseMethodCallRelationships() {
        String source = """
                package com.example;

                public class Service {
                    public void process() {
                        validate();
                        transform();
                        save();
                    }
                }
                """;

        Optional<ParsedClass> result = parser.parse("Service.java", source);

        assertThat(result).isPresent();
        ParsedMethod method = result.get().getMethods().getFirst();
        assertThat(method.getCalledMethods()).containsExactly("validate", "transform", "save");
    }

    @Test
    void shouldReturnEmptyForInvalidSource() {
        Optional<ParsedClass> result = parser.parse("invalid.java", "not valid java code @@@");
        assertThat(result).isEmpty();
    }
}
