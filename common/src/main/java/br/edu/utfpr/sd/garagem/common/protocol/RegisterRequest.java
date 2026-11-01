package br.edu.utfpr.sd.garagem.common.protocol;

public final class RegisterRequest {

    private final String method = Methods.REGISTER;
    private final Data data;

    public RegisterRequest(String name, String username, String password) {
        this.data = new Data(name, username, password, null, null);
    }

    public RegisterRequest(String name, String username, String password, String role, String token) {
        this.data = new Data(name, username, password, role, token);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getName() { return data.name; }
    public String getUsername() { return data.username; }
    public String getPassword() { return data.password; }
    public String getRole() { return data.role; }
    public String getToken() { return data.token; }

    public static final class Data {
        private final String name;
        private final String username;
        private final String password;
        private final String role;
        private final String token;

        public Data(String name, String username, String password, String role, String token) {
            this.name = name;
            this.username = username;
            this.password = password;
            this.role = role;
            this.token = token;
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public String getToken() { return token; }
    }
}
