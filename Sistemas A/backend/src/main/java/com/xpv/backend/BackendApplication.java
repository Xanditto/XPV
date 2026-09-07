package com.xpv.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) throws IOException {
		// O driver do SQLite não cria diretórios ausentes no caminho do arquivo,
		// então garantimos que "data/" exista antes do datasource ser inicializado.
		Files.createDirectories(Path.of("data"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
