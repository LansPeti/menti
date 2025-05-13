package ppke.common.dto;
import ppke.common.model.PollState;
import ppke.common.model.PollType;

import java.util.List;
/**
 * Adatátviteli Objektum (DTO) egy szavazás állapotának és adatainak továbbítására a kliens felé.
 * Tartalmazza a szavazás alapadatait, állapotát, típus-specifikus beállításait (opciók, stb.),
 * és az eredményeket (ha elérhetők és relevánsak).
 * Rekordként definiálva a tömörségért és immutabilitásért.
 * Ez az objektum {@link Response} típusként is funkcionálhat (pl. a {@link PollUpdateNotification}-ben).
 *
 * @param joinCode A szavazás egyedi, nagybetűs csatlakozási kódja.
 * @param name A szavazás neve.
 * @param question A szavazásban feltett kérdés.
 * @param type A szavazás típusa ({@link PollType}).
 * @param state A szavazás aktuális állapota ({@link PollState}).
 * @param options A válaszlehetőségek listája (csak Multiple Choice esetén releváns, egyébként null).
 * @param aspects A skála szempontjainak listája
 * @param scaleMin A skála minimum értéke
 * @param scaleMax A skála maximum értéke
 * @param results A szavazás eredményei (általában {@code Map<String, Integer>} vagy {@code Map<String, Double>}),
 * csak akkor nem null, ha az állapot {@link PollState#RESULTS}. A konkrét típus a {@code type}-tól függ.
 */
public record PollDTO(
        String joinCode,
        String name,
        String question,
        PollType type,
        PollState state,
        List<String> options,
        List<String> aspects,
        int scaleMin,
        int scaleMax,
        Object results
) implements Response { // Response interfészt implementál, mert értesítésben is használjuk
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;
    // Rekordok automatikusan generálják a szükséges metódusokat.
}