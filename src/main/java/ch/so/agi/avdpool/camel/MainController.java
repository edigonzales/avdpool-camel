package ch.so.agi.avdpool.camel;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public ResponseEntity<String>  ping() {
        return new ResponseEntity<String>("avdpool", HttpStatus.OK);
    }

}
