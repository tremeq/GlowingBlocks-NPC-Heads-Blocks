package fr.skytasul.glowingentities.libs.reflection.mappings.files;

import fr.skytasul.glowingentities.libs.reflection.mappings.Mappings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface MappingType {
   @NotNull
   Mappings parse(@NotNull List<String> var1);

   void write(@NotNull BufferedWriter var1, @NotNull Mappings var2) throws IOException;
}
