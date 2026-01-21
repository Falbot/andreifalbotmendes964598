package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.api.dto.RegionalResponses;
import br.com.falbot.seplag.backend.dominio.Regional;
import br.com.falbot.seplag.backend.repositorio.RegionalRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RegionalServico {

    private final RegionalRepositorio repo;
    private final IntegradorArgusClient client;

    public RegionalServico(RegionalRepositorio repo, IntegradorArgusClient client) {
        this.repo = repo;
        this.client = client;
    }

    @Transactional(readOnly = true)
    public List<RegionalResponses.RegionalResponse> listar(Boolean ativo, String nome) {
        boolean somenteAtivos = (ativo == null) || ativo;

        List<Regional> lista;
        if (nome != null && !nome.isBlank()) {
            lista = somenteAtivos
                    ? repo.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim())
                    : repo.findByAtivoFalseAndNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
        } else {
            lista = somenteAtivos
                    ? repo.findByAtivoTrueOrderByNomeAsc()
                    : repo.findByAtivoFalseOrderByNomeAsc();
        }

        return lista.stream()
                .map(r -> new RegionalResponses.RegionalResponse(
                        r.getIdRegional(),
                        r.getIdExterno(),
                        r.getNome(),
                        r.isAtivo(),
                        r.getCriadoEm()
                ))
                .toList();
    }

    @Transactional
    public RegionalResponses.SyncResponse sincronizar() {
        var externas = client.listarRegionais();
        int totalRecebidos = externas.size();

        //idExterno -> DTO externo
        Map<Integer, IntegradorArgusClient.RegionalExternaDTO> extMap = externas.stream()
                .filter(e -> e.id() != null && e.nome() != null)
                .collect(Collectors.toMap(
                        IntegradorArgusClient.RegionalExternaDTO::id,
                        Function.identity(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        //Ativos atuais por idExterno
        List<Regional> ativas = repo.findByAtivoTrueAndIdExternoIsNotNull();
        Map<Integer, Regional> ativosMap = ativas.stream()
                .collect(Collectors.toMap(Regional::getIdExterno, Function.identity(), (a, b) -> a));

        int inseridos = 0;
        int inativados = 0;
        int alterados = 0;

        //1) Novo no endpoint -> inserir
        //3) Alterado -> inativar antigo e criar novo
        for (var ext : extMap.values()) {
            Regional atual = ativosMap.remove(ext.id()); //remove para sobrar apenas os "ausentes"
            if (atual == null) {
                repo.save(novo(ext.id(), ext.nome()));
                inseridos++;
                continue;
            }

            String nomeAtual = normalizar(atual.getNome());
            String nomeNovo = normalizar(ext.nome());

            if (!Objects.equals(nomeAtual, nomeNovo)) {
                atual.setAtivo(false);
                repo.save(atual);
                repo.flush(); //Garantir que a inativação foi pro banco antes do INSERT
                inativados++;

                repo.save(novo(ext.id(), ext.nome()));
                inseridos++;
                alterados++;
            }
        }

        //2) Ausente no endpoint -> inativar
        for (Regional ausente : ativosMap.values()) {
            ausente.setAtivo(false);
            repo.save(ausente);
            inativados++;
        }

        int totalAtivos = repo.findByAtivoTrueOrderByNomeAsc().size();
        return new RegionalResponses.SyncResponse(inseridos, inativados, alterados, totalAtivos, totalRecebidos);
    }

    private static Regional novo(Integer idExterno, String nome) {
        Regional r = new Regional();
        r.setIdExterno(idExterno);
        r.setNome(nome == null ? null : nome.trim());
        r.setAtivo(true);
        return r;
    }

    private static String normalizar(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
