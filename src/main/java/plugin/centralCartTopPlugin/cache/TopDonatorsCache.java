package plugin.centralCartTopPlugin.cache;

import plugin.centralCartTopPlugin.model.TopCustomer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sistema de cache thread-safe para armazenar dados dos top doadores
 * Reduz chamadas à API e melhora performance
 */
public class TopDonatorsCache {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<TopCustomer> cachedData;
    private LocalDateTime lastUpdate;
    private final long cacheDurationMinutes;

    public TopDonatorsCache(long cacheDurationMinutes) {
        this.cacheDurationMinutes = cacheDurationMinutes;
        this.cachedData = new ArrayList<>();
        this.lastUpdate = LocalDateTime.MIN;
    }

    /**
     * Verifica se o cache ainda é válido
     */
    public boolean isValid() {
        lock.readLock().lock();
        try {
            if (cachedData.isEmpty()) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now();
            return lastUpdate.plusMinutes(cacheDurationMinutes).isAfter(now);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Obtém dados do cache (cópia defensiva)
     */
    public List<TopCustomer> getData() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cachedData);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Atualiza o cache com novos dados
     */
    public void update(List<TopCustomer> newData) {
        lock.writeLock().lock();
        try {
            this.cachedData = new ArrayList<>(newData);
            this.lastUpdate = LocalDateTime.now();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Limpa o cache forçadamente
     */
    public void invalidate() {
        lock.writeLock().lock();
        try {
            this.cachedData.clear();
            this.lastUpdate = LocalDateTime.MIN;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retorna o tempo desde a última atualização em minutos
     */
    public long getMinutesSinceLastUpdate() {
        lock.readLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            return java.time.Duration.between(lastUpdate, now).toMinutes();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Verifica se o cache tem dados
     */
    public boolean hasData() {
        lock.readLock().lock();
        try {
            return !cachedData.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Obtém o tempo restante de validade do cache em minutos
     */
    public long getRemainingValidityMinutes() {
        lock.readLock().lock();
        try {
            if (!hasData()) {
                return 0;
            }
            LocalDateTime expiryTime = lastUpdate.plusMinutes(cacheDurationMinutes);
            LocalDateTime now = LocalDateTime.now();
            long remaining = java.time.Duration.between(now, expiryTime).toMinutes();
            return Math.max(0, remaining);
        } finally {
            lock.readLock().unlock();
        }
    }
}

