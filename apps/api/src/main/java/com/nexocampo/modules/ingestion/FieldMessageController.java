package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.identity.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Entrada de mensagens pelo app. O WhatsApp usa o mesmo {@link FieldMessageService}, então tudo o que
 * funciona aqui funciona lá.
 */
@RestController
@RequestMapping("/api/ingestao/mensagens")
public class FieldMessageController {

    private final FieldMessageService fieldMessageService;

    public FieldMessageController(FieldMessageService fieldMessageService) {
        this.fieldMessageService = fieldMessageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FieldMessageService.FieldMessageResponse receive(
            @RequestParam(name = "texto", required = false) String texto,
            @RequestPart(name = "arquivo", required = false) MultipartFile arquivo,
            @AuthenticationPrincipal User user) {
        return fieldMessageService.receive(user, IncomingMessage.fromApp(texto, IncomingMedia.fromUpload(arquivo)));
    }

    @GetMapping
    public List<FieldMessageService.FieldMessageResponse> findMine(@AuthenticationPrincipal User user) {
        return fieldMessageService.findMine(user);
    }
}
